package dev.omatheusmesmo.demo.threading.cpu;

import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnEventLoop;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnVirtualThread;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnWorker;
import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(CpuResource.class)
class CpuTest {

    private static final String EXPECTED = Hashing.digest(CpuResource.ROUNDS);

    @Test
    void theWorkerVariantComputesOnAWorker() {
        get("/worker").then().statusCode(200).body("thread", ranOnWorker()).body("result", is(EXPECTED));
    }

    @Test
    void theReactiveVariantComputesOnTheEventLoop() {
        get("/reactive").then().statusCode(200).body("thread", ranOnEventLoop()).body("result", is(EXPECTED));
    }

    @Test
    void theVirtualVariantComputesOnAVirtualThread() {
        get("/virtual").then().statusCode(200).body("thread", ranOnVirtualThread()).body("result", is(EXPECTED));
    }
}
