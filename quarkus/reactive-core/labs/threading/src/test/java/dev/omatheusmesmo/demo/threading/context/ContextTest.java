package dev.omatheusmesmo.demo.threading.context;

import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnEventLoop;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnVirtualThread;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnWorker;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(ContextResource.class)
class ContextTest {

    private static final String TAG = "request-42";

    @Test
    void theRequestFilterRunsOnTheSameVirtualThreadAsTheEndpoint() {
        JsonPath body = tagged().get("/virtual").then().statusCode(200)
                .body("thread", ranOnVirtualThread())
                .body("result.contextLocal", is(TAG))
                .body("result.threadLocal", is(TAG))
                .extract().jsonPath();

        sameThread(body);
    }

    @Test
    void theRequestFilterRunsOnTheSameWorkerAsTheEndpoint() {
        JsonPath body = tagged().get("/worker").then().statusCode(200)
                .body("thread", ranOnWorker())
                .body("result.contextLocal", is(TAG))
                .body("result.threadLocal", is(TAG))
                .extract().jsonPath();

        sameThread(body);
    }

    @Test
    void theRequestFilterRunsOnTheSameEventLoopAsTheEndpoint() {
        JsonPath body = tagged().get("/event-loop").then().statusCode(200)
                .body("thread", ranOnEventLoop())
                .body("result.contextLocal", is(TAG))
                .body("result.threadLocal", is(TAG))
                .extract().jsonPath();

        sameThread(body);
    }

    @Test
    void contextLocalsFollowTheWorkToAnotherThreadButThreadLocalsStayBehind() {
        tagged().get("/hop").then().statusCode(200)
                .body("thread", ranOnWorker())
                .body("result.filterThread", startsWith("vert.x-eventloop-thread-"))
                .body("result.contextLocal", is(TAG))
                .body("result.threadLocal", nullValue());
    }

    @Test
    void theRootContextRefusesLocals() {
        get("/root").then().statusCode(500)
                .body("error", is("UnsupportedOperationException"))
                .body("message", containsString("root"));
    }

    private static RequestSpecification tagged() {
        return given().header(RequestTagFilter.HEADER, TAG);
    }

    private static void sameThread(JsonPath body) {
        org.hamcrest.MatcherAssert.assertThat(body.getString("result.filterThread"), is(body.getString("thread.name")));
    }
}
