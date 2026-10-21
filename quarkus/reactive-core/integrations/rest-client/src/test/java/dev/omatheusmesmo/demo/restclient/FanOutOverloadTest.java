package dev.omatheusmesmo.demo.restclient;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.omatheusmesmo.demo.testsupport.InjectSlowBackend;
import dev.omatheusmesmo.demo.testsupport.SlowBackend;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;

/**
 * The fan-out benchmark scaled down by ten: 5 connections to a 100 ms upstream carry 50 calls, so 25 requests, per
 * second, and the load asks for 30, at a fixed rate, whatever the server answers. Twenty workers let at most 40 calls
 * wait for the 5 connections, about 0.8 s, under the one second deadline; the extra requests wait for a worker,
 * before their deadline starts.
 *
 * <p>The reactive and virtual thread variants let every call wait for a connection. Once that wait nears the
 * deadline, a call leaves the queue in time to be sent but its answer comes too late: the request fails, the upstream
 * still does the work, and cancelling a call already sent closes its HTTP/1.1 connection, so the pool opens another
 * one and the upstream receives more calls per second than the 5 connections should carry.
 */
@QuarkusTest
@TestProfile(FanOutOverloadTest.SmallPools.class)
@WithTestResource(value = SlowBackend.class, initArgs = {
        @ResourceArg(name = SlowBackend.PROPERTY, value = "quarkus.rest-client.upstream.url"),
        @ResourceArg(name = SlowBackend.DELAY_MS, value = "100"),
        @ResourceArg(name = SlowBackend.BODY, value = "{\"text\":\"stay reactive\"}")
})
class FanOutOverloadTest {

    private static final int CONNECTIONS = 5;
    private static final int WORKERS = 20;
    private static final int UPSTREAM_DELAY_MS = 100;
    private static final int CALLS_PER_SECOND = CONNECTIONS * 1000 / UPSTREAM_DELAY_MS;
    private static final int RATE = 30;
    private static final int REQUESTS = RATE * 10;
    private static final Duration CLIENT_TIMEOUT = Duration.ofSeconds(10);

    @InjectSlowBackend
    WireMockServer upstream;

    @TestHTTPEndpoint(FanOutResource.class)
    @TestHTTPResource
    URL fanOut;

    @Test
    void theWorkerVariantServesEveryRequestAndWastesNoCall() {
        Outcome outcome = overload("/worker");

        assertThat(outcome.succeeded(), is(REQUESTS));
        assertThat(outcome.upstreamCalls(), is(2 * outcome.succeeded()));
    }

    @Test
    void theReactiveVariantSendsEveryCallButMostAnswersComeTooLate() {
        collapses(overload("/reactive"));
    }

    @Test
    void theVirtualVariantSendsEveryCallButMostAnswersComeTooLate() {
        collapses(overload("/virtual"));
    }

    private static void collapses(Outcome outcome) {
        assertThat(outcome.succeeded(), lessThan(REQUESTS / 2));
        assertThat(outcome.upstreamCalls(), is(2 * REQUESTS));
        assertThat(outcome.busiestUpstreamSecond(), greaterThan((long) CALLS_PER_SECOND));
    }

    private Outcome overload(String path) {
        settle();
        upstream.resetRequests();
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(fanOut + path)).timeout(CLIENT_TIMEOUT).build();
        List<CompletableFuture<Integer>> responses = new ArrayList<>();
        long interval = Duration.ofSeconds(1).toNanos() / RATE;
        long start = System.nanoTime();
        for (int i = 0; i < REQUESTS; i++) {
            LockSupport.parkNanos(start + i * interval - System.nanoTime());
            responses.add(client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenApply(HttpResponse::statusCode)
                    .exceptionally(failure -> 0));
        }
        int succeeded = (int) responses.stream().map(CompletableFuture::join).filter(status -> status == 200).count();
        settle();
        return new Outcome(succeeded, upstreamCalls(), busiestUpstreamSecond());
    }

    /**
     * Waits until the upstream count stops moving, so no call from an earlier test lands in the next one.
     */
    private void settle() {
        int previous;
        int current = upstreamCalls();
        do {
            LockSupport.parkNanos(Duration.ofSeconds(2).toNanos());
            previous = current;
            current = upstreamCalls();
        } while (current != previous);
    }

    private int upstreamCalls() {
        return upstream.countRequestsMatching(anyRequestedFor(anyUrl()).build()).getCount();
    }

    private long busiestUpstreamSecond() {
        return Collections.max(upstream.getAllServeEvents().stream()
                .collect(groupingBy(event -> event.getRequest().getLoggedDate().getTime() / 1000, counting()))
                .values());
    }

    record Outcome(int succeeded, int upstreamCalls, long busiestUpstreamSecond) {
    }

    public static class SmallPools implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.rest-client.upstream.connection-pool-size", String.valueOf(CONNECTIONS),
                    "quarkus.thread-pool.max-threads", String.valueOf(WORKERS));
        }
    }
}
