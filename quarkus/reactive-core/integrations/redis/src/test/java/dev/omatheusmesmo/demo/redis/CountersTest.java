package dev.omatheusmesmo.demo.redis;

import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnEventLoop;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnVirtualThread;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnWorker;
import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(CountersResource.class)
class CountersTest {

    @Test
    void theBlockingClientIncrementsFromAWorker() {
        incrementsOnce("/worker", ranOnWorker());
    }

    @Test
    void theReactiveClientIncrementsOnTheEventLoop() {
        incrementsOnce("/reactive", ranOnEventLoop());
    }

    @Test
    void theBlockingClientIncrementsFromAVirtualThread() {
        incrementsOnce("/virtual", ranOnVirtualThread());
    }

    private static void incrementsOnce(String path, Matcher<Object> thread) {
        long before = count(get(path).then().statusCode(200).body("thread", thread));
        get(path).then().statusCode(200).body("thread", thread).body("result", is((int) before + 1));
    }

    private static long count(ValidatableResponse response) {
        return response.extract().jsonPath().getLong("result");
    }
}
