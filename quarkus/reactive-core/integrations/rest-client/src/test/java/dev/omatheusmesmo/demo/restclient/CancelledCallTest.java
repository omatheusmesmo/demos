package dev.omatheusmesmo.demo.restclient;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

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
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;

/**
 * Ten requests, so twenty calls, share one connection to a 300 ms upstream. Before the one second deadline the
 * connection carries at most four calls; the deadline cancels the rest while they still wait for it, and the
 * REST Client never sends them.
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

    @InjectSlowBackend
    WireMockServer upstream;

    @Test
    void aCallCancelledWhileWaitingForAConnectionIsNeverSent() {
        Concurrently.get(REQUESTS, "/reactive");
        LockSupport.parkNanos(Duration.ofSeconds(3).toNanos());

        assertThat(upstream.countRequestsMatching(anyRequestedFor(anyUrl()).build()).getCount(),
                lessThanOrEqualTo(CALLS_BEFORE_THE_DEADLINE));
    }

    public static class OneConnection implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.rest-client.upstream.connection-pool-size", "1",
                    "quarkus.rest-client.upstream.read-timeout", "10000");
        }
    }
}
