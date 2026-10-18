package dev.omatheusmesmo.demo.restclient;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import dev.omatheusmesmo.demo.testsupport.SlowBackend;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The REST Client read timeout fires before {@code ifNoItem()}, so the reactive variant fails with the Vert.x
 * timeout instead of the Mutiny one. Vert.x raises a checked {@code java.util.concurrent.TimeoutException}, which
 * an exception mapper declared for {@code RuntimeException} never sees.
 */
@QuarkusTest
@TestProfile(ClientTimeoutFirstTest.ShortReadTimeout.class)
@WithTestResource(value = SlowBackend.class, initArgs = {
        @ResourceArg(name = SlowBackend.PROPERTY, value = "quarkus.rest-client.upstream.url"),
        @ResourceArg(name = SlowBackend.DELAY_MS, value = "2000")
})
class ClientTimeoutFirstTest {

    @Test
    void aCheckedTimeoutFromTheReactiveClientIsStillAGatewayTimeout() {
        get("/quotes/reactive").then().statusCode(504)
                .body("error", is("NoStackTraceTimeoutException"));
    }

    public static class ShortReadTimeout implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.rest-client.upstream.read-timeout", "300");
        }
    }
}
