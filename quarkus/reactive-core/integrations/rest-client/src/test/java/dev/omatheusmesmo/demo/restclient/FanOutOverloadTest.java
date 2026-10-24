package dev.omatheusmesmo.demo.restclient;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.oneOf;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.omatheusmesmo.demo.testsupport.InjectSlowBackend;
import dev.omatheusmesmo.demo.testsupport.OpenLoad;
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
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;

/**
 * The fan-out benchmark scaled down: 5 connections to a 100 ms upstream carry 50 calls, so 25 requests, per second,
 * and the load asks for 30, at a fixed rate, whatever the server answers. Ten workers let at most 20 calls wait for
 * the 5 connections, so a call is answered at most 0.5 s after it starts waiting, under the one second deadline even
 * when the upstream runs slower than its 100 ms; the extra requests wait for a worker, before their deadline starts.
 * Each variant first runs a few seconds well under capacity, so a cold JIT does not count as overload, and the read
 * timeout is longer than the deadline, so the deadline is the only thing that fails a request.
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
    private static final int WORKERS = 10;
    private static final int UPSTREAM_DELAY_MS = 100;
    private static final int CALLS_PER_SECOND = CONNECTIONS * 1000 / UPSTREAM_DELAY_MS;
    private static final int RATE = 30;
    private static final int REQUESTS = RATE * 10;
    private static final int WARM_UP_RATE = 10;
    private static final int DEADLINE_STATUS = 504;
    private static final Duration CLIENT_TIMEOUT = Duration.ofSeconds(10);

    @InjectSlowBackend
    WireMockServer upstream;

    @TestHTTPEndpoint(FanOutResource.class)
    @TestHTTPResource
    URL fanOut;

    @Test
    void theWorkerVariantServesEveryRequestAndWastesNoCall() {
        Outcome outcome = overload("/worker");

        assertThat(outcome.statuses(), everyItem(is(200)));
        assertThat(outcome.upstreamCalls(), is(2 * REQUESTS));
        assertThat(outcome.busiestUpstreamSecond(), lessThanOrEqualTo((long) CALLS_PER_SECOND));
    }

    @Test
    void theReactiveVariantSendsNearlyEveryCallButMostAnswersComeTooLate() {
        collapses(overload("/reactive"));
    }

    @Test
    void theVirtualVariantSendsNearlyEveryCallButMostAnswersComeTooLate() {
        collapses(overload("/virtual"));
    }

    private static void collapses(Outcome outcome) {
        assertThat(outcome.statuses(), everyItem(oneOf(200, DEADLINE_STATUS)));
        assertThat(outcome.succeeded(), greaterThan(0L));
        assertThat(outcome.succeeded(), lessThan((long) REQUESTS / 2));
        assertThat(outcome.upstreamCalls(), greaterThanOrEqualTo(2 * REQUESTS * 9 / 10));
        assertThat(outcome.busiestUpstreamSecond(), greaterThan((long) CALLS_PER_SECOND));
    }

    private Outcome overload(String path) {
        URI endpoint = URI.create(fanOut + path);
        OpenLoad.get(endpoint, WARM_UP_RATE, WARM_UP_RATE * 3, CLIENT_TIMEOUT);
        settle();
        upstream.resetRequests();
        List<Integer> statuses = OpenLoad.get(endpoint, RATE, REQUESTS, CLIENT_TIMEOUT);
        settle();
        Outcome outcome = new Outcome(statuses, upstreamCalls(), busiestUpstreamSecond());
        System.out.printf("%s: %d of %d succeeded, %d upstream calls, at most %d in one second%n", path,
                outcome.succeeded(), REQUESTS, outcome.upstreamCalls(), outcome.busiestUpstreamSecond());
        return outcome;
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
        return upstream.getAllServeEvents().stream()
                .collect(groupingBy(event -> event.getRequest().getLoggedDate().getTime() / 1000, counting()))
                .values().stream().mapToLong(Long::longValue).max().orElse(0);
    }

    record Outcome(List<Integer> statuses, int upstreamCalls, long busiestUpstreamSecond) {

        long succeeded() {
            return statuses.stream().filter(status -> status == 200).count();
        }
    }

    public static class SmallPools implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.rest-client.upstream.connection-pool-size", String.valueOf(CONNECTIONS),
                    "quarkus.rest-client.upstream.read-timeout", "10000",
                    "quarkus.thread-pool.max-threads", String.valueOf(WORKERS));
        }
    }
}
