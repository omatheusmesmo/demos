package dev.omatheusmesmo.demo.restclient;

import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnEventLoop;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnVirtualThread;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnWorker;
import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
@SlowUpstream
@TestHTTPEndpoint(QuotesResource.class)
class QuotesTest {

    @Test
    void theWorkerVariantCallsTheUpstreamFromAWorker() {
        get("/worker").then().statusCode(200)
                .body("thread", ranOnWorker())
                .body("result", hasSize(2))
                .body("result.text", everyItem(is("stay reactive")));
    }

    @Test
    void theReactiveVariantCombinesBothCallsOnTheEventLoop() {
        get("/reactive").then().statusCode(200)
                .body("thread", ranOnEventLoop())
                .body("result", hasSize(2))
                .body("result.text", everyItem(is("stay reactive")));
    }

    @Test
    void theVirtualVariantAwaitsBothCallsOnAVirtualThread() {
        get("/virtual").then().statusCode(200)
                .body("thread", ranOnVirtualThread())
                .body("result", hasSize(2))
                .body("result.text", everyItem(is("stay reactive")));
    }
}
