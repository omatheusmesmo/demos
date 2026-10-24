package dev.omatheusmesmo.demo.restclient;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.oneOf;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.omatheusmesmo.demo.testsupport.Concurrently;
import dev.omatheusmesmo.demo.testsupport.InjectSlowBackend;
import dev.omatheusmesmo.demo.testsupport.SlowBackend;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;

/**
 * After one request warms the endpoint up, ten requests, so twenty calls, share one connection to a 300 ms upstream. Before the one second deadline the
 * connection carries four calls, the fourth one still in flight when the deadline fires. The deadline cancels the
 * other sixteen while they still wait for the connection, and the REST Client never sends them, although the queue
 * would have drained them in six seconds.
 *
 * <p>Cancelling the fourth call closes the connection, and the pool may open a new one for the next waiter a moment
 * before that waiter's own cancellation lands, so one more call can slip out.
 */
@QuarkusTest
@TestHTTPEndpoint(FanOutResource.class)
@TestProfile(CancelledCallTest.OneConnection.class)
@WithTestResource(value = SlowBackend.class, initArgs = {
        @ResourceArg(name = SlowBackend.PROPERTY, value = "quarkus.rest-client.upstream.url"),
        @ResourceArg(name = SlowBackend.DELAY_MS, value = "300")
})
class CancelledCallTest {

    private static final int REQUESTS = 10;
    private static final int CALLS_BEFORE_THE_DEADLINE = 4;
    private static final int RACING_THE_RECONNECT = 1;
    private static final Duration TIME_TO_DRAIN_EVERY_CALL = Duration.ofSeconds(7);

    @InjectSlowBackend
    WireMockServer upstream;

    @Test
    void aCallCancelledWhileWaitingForAConnectionIsNeverSent() {
        RestAssured.get("/reactive").then().statusCode(200);
        upstream.resetRequests();

        List<Integer> statuses = Concurrently.get(REQUESTS, "/reactive").stream().map(Response::statusCode).toList();
        LockSupport.parkNanos(TIME_TO_DRAIN_EVERY_CALL.toNanos());

        assertThat(statuses, everyItem(oneOf(200, 504)));
        assertThat(upstream.countRequestsMatching(anyRequestedFor(anyUrl()).build()).getCount(),
                is(both(greaterThan(0)).and(lessThanOrEqualTo(CALLS_BEFORE_THE_DEADLINE + RACING_THE_RECONNECT))));
    }

    public static class OneConnection implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.rest-client.upstream.connection-pool-size", "1",
                    "quarkus.rest-client.upstream.read-timeout", "10000");
        }
    }
}
