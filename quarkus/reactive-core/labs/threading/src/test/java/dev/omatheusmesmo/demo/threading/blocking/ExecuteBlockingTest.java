package dev.omatheusmesmo.demo.threading.blocking;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(ExecuteBlockingResource.class)
class ExecuteBlockingTest {

    @Test
    void blockingTasksFromTheSameContextRunOneAtATimeByDefault() {
        get("/ordered").then().statusCode(200).body("result", is(1));
    }

    @Test
    void unorderedBlockingTasksFromTheSameContextOverlap() {
        get("/unordered").then().statusCode(200).body("result", is(2));
    }
}
