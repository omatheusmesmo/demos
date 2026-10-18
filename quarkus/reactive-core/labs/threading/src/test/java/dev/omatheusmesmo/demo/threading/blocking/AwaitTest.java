package dev.omatheusmesmo.demo.threading.blocking;

import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnVirtualThread;
import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(AwaitResource.class)
class AwaitTest {

    @Test
    void awaitingOnAnEventLoopIsRefused() {
        get("/event-loop").then().statusCode(500)
                .body("message", containsString("cannot be blocked"));
    }

    @Test
    void awaitingOnAVirtualThreadIsFine() {
        get("/virtual").then().statusCode(200)
                .body("thread", ranOnVirtualThread())
                .body("result", is("done"));
    }
}
