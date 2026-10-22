package dev.omatheusmesmo.demo.postgres;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import dev.omatheusmesmo.demo.common.ConcurrencyTracker;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;

/**
 * Both pools get 5 connections, so each model can run 25 of the 200 ms queries per second, and the load asks for
 * 50, at a fixed rate, from clients that give up after 2 s. No endpoint has a deadline of its own.
 *
 * <p>Every model then runs queries for requests whose client has already left, and more than half of the database
 * work is wasted. What differs is how long the database stays busy after the last client gave up: the worker pool
 * lets at most 20 requests wait, Agroal fails a wait for a JDBC connection after 5 s, and the wait queue of the
 * reactive pool has no limit, so it keeps sending queries nobody will read.
 *
 * <p>Queries are counted as transactions in {@code pg_stat_database}, minus the ones the test sends to observe.
 */
@QuarkusTest
@TestProfile(AbandonedQueryTest.FivePerPool.class)
class AbandonedQueryTest {

    private static final int RATE = 50;
    private static final int REQUESTS = RATE * 10;
    private static final double AGROAL_ACQUISITION_TIMEOUT_S = 5;

    @Inject
    ConcurrencyTracker inFlight;

    @Inject
    AgroalDataSource dataSource;

    @TestHTTPResource("/books")
    URL books;

    private static final int COMMITS_QUERY_TRANSACTIONS = 2;

    private int monitoringQueries;

    @Test
    void theWorkerPoolLimitsHowLongTheDatabaseKeepsWorkingForNobody() throws Exception {
        Outcome outcome = overload("/worker");

        wastesMostQueries(outcome);
        assertThat(outcome.tailSeconds(), lessThan(2.0));
    }

    @Test
    void theAcquisitionTimeoutLimitsItForVirtualThreads() throws Exception {
        Outcome outcome = overload("/virtual");

        wastesMostQueries(outcome);
        assertThat(outcome.tailSeconds(), lessThan(AGROAL_ACQUISITION_TIMEOUT_S));
    }

    @Test
    void theReactivePoolKeepsQueuingQueriesForClientsThatLeft() throws Exception {
        Outcome outcome = overload("/reactive");

        wastesMostQueries(outcome);
        assertThat(outcome.tailSeconds(), greaterThan(AGROAL_ACQUISITION_TIMEOUT_S));
    }

    private static void wastesMostQueries(Outcome outcome) {
        assertThat(outcome.succeeded(), lessThan((long) REQUESTS / 2));
        assertThat(outcome.queries(), greaterThan(2 * outcome.succeeded()));
    }

    private Outcome overload(String path) throws Exception {
        while (inFlight.inFlight() > 0 || sleepingQueries() > 0) {
            LockSupport.parkNanos(Duration.ofMillis(500).toNanos());
        }
        LockSupport.parkNanos(Duration.ofSeconds(1).toNanos());
        long before = commits();
        monitoringQueries = 0;
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(books + path)).timeout(Duration.ofSeconds(2)).build();
        List<CompletableFuture<Integer>> responses = new ArrayList<>();
        long interval = Duration.ofSeconds(1).toNanos() / RATE;
        long start = System.nanoTime();
        for (int i = 0; i < REQUESTS; i++) {
            LockSupport.parkNanos(start + i * interval - System.nanoTime());
            responses.add(client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenApply(HttpResponse::statusCode).exceptionally(failure -> 0));
        }
        long succeeded = responses.stream().map(CompletableFuture::join).filter(status -> status == 200).count();
        long allGaveUp = System.nanoTime();
        int sleeping = sleepingQueries();
        while (inFlight.inFlight() > 0 || sleeping > 0) {
            LockSupport.parkNanos(Duration.ofMillis(100).toNanos());
            sleeping = sleepingQueries();
        }
        double tailSeconds = (System.nanoTime() - allGaveUp) / 1e9;
        LockSupport.parkNanos(Duration.ofSeconds(2).toNanos());
        long queries = commits() - before - monitoringQueries - COMMITS_QUERY_TRANSACTIONS;
        return new Outcome(succeeded, queries, tailSeconds);
    }

    private int sleepingQueries() throws SQLException {
        monitoringQueries++;
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "select count(*) from pg_stat_activity where state = 'active' and query like '%pg_sleep%'"
                                + " and pid <> pg_backend_pid()")) {
            result.next();
            return result.getInt(1);
        }
    }

    private long commits() throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("select pg_stat_clear_snapshot()");
            try (ResultSet result = statement.executeQuery(
                    "select xact_commit + xact_rollback from pg_stat_database where datname = current_database()")) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    record Outcome(long succeeded, long queries, double tailSeconds) {
    }

    public static class FivePerPool implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.datasource.reactive.max-size", "5",
                    "quarkus.datasource.jdbc.max-size", "5",
                    "quarkus.thread-pool.max-threads", "20");
        }
    }
}
