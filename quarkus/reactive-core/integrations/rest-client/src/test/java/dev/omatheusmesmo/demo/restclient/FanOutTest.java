package dev.omatheusmesmo.demo.restclient;

import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnEventLoop;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnVirtualThread;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnWorker;
import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

/**
 * The benchmark endpoints do the same work in every model: both upstream calls start at once, so a request takes
 * one upstream delay (300 ms), never two. Two calls in a row would need at least 600 ms.
 */
@QuarkusTest
@SlowUpstream
@TestHTTPEndpoint(FanOutResource.class)
class FanOutTest {

    private static final long TWO_CALLS_IN_A_ROW_MS = 600;

    @Test
    void theWorkerVariantWaitsForBothCallsAtOnceOnAWorker() {
        overlapsBothCalls("/worker", ranOnWorker());
    }

    @Test
    void theReactiveVariantCombinesBothCallsOnTheEventLoop() {
        overlapsBothCalls("/reactive", ranOnEventLoop());
    }

    @Test
    void theVirtualVariantWaitsForBothCallsAtOnceOnAVirtualThread() {
        overlapsBothCalls("/virtual", ranOnVirtualThread());
    }

    private static void overlapsBothCalls(String path, Matcher<?> thread) {
        get(path).then().statusCode(200);
        get(path).then().statusCode(200)
                .time(lessThan(TWO_CALLS_IN_A_ROW_MS))
                .body("thread", thread)
                .body("result", hasSize(2));
    }
}
