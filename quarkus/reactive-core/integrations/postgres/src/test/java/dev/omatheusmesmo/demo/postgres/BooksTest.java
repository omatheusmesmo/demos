package dev.omatheusmesmo.demo.postgres;

import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnEventLoop;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnVirtualThread;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnWorker;
import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.contains;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(BooksResource.class)
class BooksTest {

    @Test
    void hibernateOrmRunsTheSlowQueryOnAWorker() {
        get("/worker").then().statusCode(200)
                .body("thread", ranOnWorker())
                .body("result", contains(BooksResource.TITLES));
    }

    @Test
    void hibernateReactiveRunsTheSlowQueryOnTheEventLoop() {
        get("/reactive").then().statusCode(200)
                .body("thread", ranOnEventLoop())
                .body("result", contains(BooksResource.TITLES));
    }

    @Test
    void hibernateOrmRunsTheSlowQueryOnAVirtualThread() {
        get("/virtual").then().statusCode(200)
                .body("thread", ranOnVirtualThread())
                .body("result", contains(BooksResource.TITLES));
    }
}
