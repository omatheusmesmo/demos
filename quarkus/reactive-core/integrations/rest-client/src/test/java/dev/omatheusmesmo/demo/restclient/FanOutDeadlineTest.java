package dev.omatheusmesmo.demo.restclient;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import dev.omatheusmesmo.demo.testsupport.SlowBackend;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * An upstream slower than the one second deadline, with a client read timeout long enough never to fire first.
 * Every variant fails through the same deadline, with the same Mutiny {@code TimeoutException}, well before the
 * upstream answers: the benchmark compares the models, not three different timeout mechanisms.
 */
@QuarkusTest
@TestHTTPEndpoint(FanOutResource.class)
@TestProfile(FanOutDeadlineTest.LongReadTimeout.class)
@WithTestResource(value = SlowBackend.class, initArgs = {
        @ResourceArg(name = SlowBackend.PROPERTY, value = "quarkus.rest-client.upstream.url"),
        @ResourceArg(name = SlowBackend.DELAY_MS, value = "3000")
})
class FanOutDeadlineTest {

    private static final long BEFORE_THE_UPSTREAM_ANSWERS_MS = 2500;

    @Test
    void theWorkerVariantFailsAtTheSharedDeadline() {
        failsAtTheDeadline("/worker");
    }

    @Test
    void theReactiveVariantFailsAtTheSharedDeadline() {
        failsAtTheDeadline("/reactive");
    }

    @Test
    void theVirtualVariantFailsAtTheSharedDeadline() {
        failsAtTheDeadline("/virtual");
    }

    private static void failsAtTheDeadline(String path) {
        get(path).then().statusCode(504)
                .time(lessThan(BEFORE_THE_UPSTREAM_ANSWERS_MS))
                .body("error", is("TimeoutException"));
    }

    public static class LongReadTimeout implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.rest-client.upstream.read-timeout", "10000");
        }
    }
}
