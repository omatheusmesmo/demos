package dev.omatheusmesmo.demo.postgres;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.oneOf;

import dev.omatheusmesmo.demo.common.ConcurrencyTracker;
import dev.omatheusmesmo.demo.testsupport.OpenLoad;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

/**
 * Both pools get 5 connections, so each model can run 25 of the 200 ms queries per second, and the load asks for
 * 50, at a fixed rate, from clients that give up after 2 s by closing their connection. No endpoint has a deadline
 * of its own.
 *
 * <p>Every model then runs queries for requests whose client has already left, and more than half of the database
 * work is wasted. What differs is how long the database stays busy after the last client gave up:
 * <ul>
 * <li>a request still waiting for one of the 20 workers when its client closes the connection never runs: Quarkus
 * REST checks the connection before it invokes the method, so only the requests already on a worker keep querying
 * (a proxy that keeps the backend connection open would hide the close and defeat it);</li>
 * <li>every virtual thread request starts at once and waits for a JDBC connection, until Agroal gives up after 5 s;</li>
 * <li>every reactive request starts at once too, and the reactive pool queues its query with no limit on the queue,
 * so it keeps sending queries nobody will read.</li>
 * </ul>
 *
 * <p>Queries are counted as transactions in {@code pg_stat_database}, minus the ones the test sends to observe,
 * through a connection of its own so the observing never waits behind the load. A backend reports its counters at
 * most once a second, and an idle one up to 10 s later, so every reading waits for that first.
 */
@QuarkusTest
@TestProfile(AbandonedQueryTest.FivePerPool.class)
class AbandonedQueryTest {

    private static final int CONNECTIONS = 5;
    private static final int WORKERS = 20;
    private static final int RATE = 50;
    private static final int REQUESTS = RATE * 10;
    private static final Duration CLIENT_TIMEOUT = Duration.ofSeconds(2);
    private static final double ACQUISITION_TIMEOUT_S = 5;
    private static final Duration STATS_REPORTED = Duration.ofSeconds(11);
    private static final int READING_TRANSACTIONS = 1;

    @Inject
    ConcurrencyTracker inFlight;

    @TestHTTPResource("/books")
    URL books;

    private int observingQueries;

    @Test
    void requestsStillWaitingForAWorkerWhenTheirClientLeavesNeverRun() throws Exception {
        Outcome outcome = overload("/worker");

        wastesMostQueries(outcome);
        assertThat(outcome.started(), lessThan(REQUESTS * 3 / 4));
        assertThat(outcome.tailSeconds(), lessThan(2.0));
    }

    @Test
    void theAcquisitionTimeoutLimitsHowLongVirtualThreadsKeepQuerying() throws Exception {
        Outcome outcome = overload("/virtual");

        wastesMostQueries(outcome);
        assertThat(outcome.started(), is(REQUESTS));
        assertThat(outcome.tailSeconds(), lessThan(ACQUISITION_TIMEOUT_S));
    }

    @Test
    void theReactivePoolKeepsQueuingQueriesForClientsThatLeft() throws Exception {
        Outcome outcome = overload("/reactive");

        wastesMostQueries(outcome);
        assertThat(outcome.started(), is(REQUESTS));
        assertThat(outcome.tailSeconds(), greaterThan(ACQUISITION_TIMEOUT_S));
    }

    private static void wastesMostQueries(Outcome outcome) {
        assertThat(outcome.statuses(), everyItem(oneOf(200, OpenLoad.NO_RESPONSE)));
        assertThat(outcome.succeeded(), greaterThan(0L));
        assertThat(outcome.succeeded(), lessThan((long) REQUESTS / 2));
        assertThat(outcome.queries(), greaterThan(2 * outcome.succeeded()));
    }

    private Outcome overload(String path) throws SQLException {
        try (Connection observer = observer()) {
            while (inFlight.inFlight() > 0 || sleepingQueries(observer) > 0) {
                LockSupport.parkNanos(Duration.ofMillis(500).toNanos());
            }
            LockSupport.parkNanos(STATS_REPORTED.toNanos());
            long before = transactions(observer);
            observingQueries = 0;
            inFlight.reset();

            List<Integer> statuses = OpenLoad.get(URI.create(books + path), RATE, REQUESTS, CLIENT_TIMEOUT);
            long allGaveUp = System.nanoTime();
            while (inFlight.inFlight() > 0 || sleepingQueries(observer) > 0) {
                LockSupport.parkNanos(Duration.ofMillis(100).toNanos());
            }
            double tailSeconds = (System.nanoTime() - allGaveUp) / 1e9;

            LockSupport.parkNanos(STATS_REPORTED.toNanos());
            long queries = transactions(observer) - before - observingQueries - READING_TRANSACTIONS;
            Outcome outcome = new Outcome(statuses, inFlight.started(), queries, tailSeconds);
            System.out.printf("%s: %d of %d succeeded, %d methods started, %d queries, %.1f s tail%n", path,
                    outcome.succeeded(), REQUESTS, outcome.started(), outcome.queries(), outcome.tailSeconds());
            return outcome;
        }
    }

    private static Connection observer() throws SQLException {
        Config config = ConfigProvider.getConfig();
        return DriverManager.getConnection(config.getValue("quarkus.datasource.jdbc.url", String.class),
                config.getValue("quarkus.datasource.username", String.class),
                config.getValue("quarkus.datasource.password", String.class));
    }

    private int sleepingQueries(Connection observer) throws SQLException {
        observingQueries++;
        try (Statement statement = observer.createStatement(); ResultSet result = statement.executeQuery(
                "select count(*) from pg_stat_activity where state = 'active' and query like '%pg_sleep%'"
                        + " and pid <> pg_backend_pid()")) {
            result.next();
            return result.getInt(1);
        }
    }

    private static long transactions(Connection observer) throws SQLException {
        try (Statement statement = observer.createStatement(); ResultSet result = statement.executeQuery(
                "select xact_commit + xact_rollback from pg_stat_database where datname = current_database()")) {
            result.next();
            return result.getLong(1);
        }
    }

    record Outcome(List<Integer> statuses, int started, long queries, double tailSeconds) {

        long succeeded() {
            return statuses.stream().filter(status -> status == 200).count();
        }
    }

    public static class FivePerPool implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.datasource.reactive.max-size", String.valueOf(CONNECTIONS),
                    "quarkus.datasource.jdbc.max-size", String.valueOf(CONNECTIONS),
                    "quarkus.datasource.jdbc.acquisition-timeout", (int) ACQUISITION_TIMEOUT_S + "S",
                    "quarkus.thread-pool.max-threads", String.valueOf(WORKERS));
        }
    }
}
