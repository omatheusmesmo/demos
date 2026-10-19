package dev.omatheusmesmo.demo.threading.dispatch;

import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnDuplicatedContext;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnEventLoop;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnVirtualThread;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnWorker;
import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(DispatchResource.class)
class DispatchTest {

    @Test
    void aSynchronousSignatureRunsOnAWorker() {
        get("/sync").then().statusCode(200)
                .body("thread", ranOnWorker())
                .body("thread", ranOnDuplicatedContext());
    }

    @Test
    void aUniSignatureRunsOnAnEventLoop() {
        get("/uni").then().statusCode(200)
                .body("thread", ranOnEventLoop())
                .body("thread", ranOnDuplicatedContext());
    }

    /**
     * Quarkus REST used to treat {@code @Transactional} as {@code @Blocking}. That rule was removed when
     * {@code @Transactional} started supporting Hibernate Reactive (quarkusio/quarkus#51063), so the {@code Uni}
     * signature now wins and JTA refuses to start on the event loop.
     */
    @Test
    void transactionalNoLongerSendsAUniSignatureToAWorker() {
        get("/transactional-uni").then().statusCode(500)
                .body("message", containsString("cannot start a JTA transaction within a reactive pipeline"));
    }

    @Test
    void blockingSendsAUniSignatureToAWorker() {
        get("/blocking-uni").then().statusCode(200).body("thread", ranOnWorker());
    }

    @Test
    void nonBlockingKeepsASynchronousSignatureOnTheEventLoop() {
        get("/non-blocking").then().statusCode(200).body("thread", ranOnEventLoop());
    }

    /**
     * The virtual thread guide says {@code @RunOnVirtualThread} can only be used on endpoints that are blocking by
     * annotation or signature. Quarkus REST only rejects it when the method is also marked {@code @NonBlocking}.
     */
    @Test
    void runOnVirtualThreadAlsoMovesAUniSignatureToAVirtualThread() {
        get("/virtual-uni").then().statusCode(200)
                .body("thread", ranOnVirtualThread())
                .body("thread", ranOnDuplicatedContext());
    }

    @Test
    void runOnVirtualThreadUsesANamedVirtualThread() {
        get("/virtual").then().statusCode(200)
                .body("thread", ranOnVirtualThread())
                .body("thread", ranOnDuplicatedContext())
                .body("thread.name", startsWith("quarkus-virtual-thread-"));
    }
}
