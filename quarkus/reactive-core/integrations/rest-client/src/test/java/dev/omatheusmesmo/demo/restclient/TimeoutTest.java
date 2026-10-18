package dev.omatheusmesmo.demo.restclient;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import dev.omatheusmesmo.demo.testsupport.SlowBackend;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * An upstream slower than the one second timeout. Each variant times out with its own mechanism: the REST Client
 * read timeout (worker), {@code ifNoItem().after()} (reactive) and {@code await().atMost()} (virtual thread).
 */
@QuarkusTest
@TestHTTPEndpoint(QuotesResource.class)
@WithTestResource(value = SlowBackend.class, initArgs = {
        @ResourceArg(name = SlowBackend.PROPERTY, value = "quarkus.rest-client.upstream.url"),
        @ResourceArg(name = SlowBackend.DELAY_MS, value = "2000")
})
class TimeoutTest {

    @Test
    void theWorkerVariantTimesOutThroughTheRestClient() {
        gatewayTimeout("/worker");
    }

    @Test
    void theReactiveVariantTimesOutThroughIfNoItem() {
        gatewayTimeout("/reactive");
    }

    @Test
    void theVirtualVariantTimesOutThroughAwaitAtMost() {
        gatewayTimeout("/virtual");
    }

    private static void gatewayTimeout(String path) {
        get(path).then().statusCode(504)
                .body("status", is(504))
                .body("error", containsString("Timeout"));
    }
}
