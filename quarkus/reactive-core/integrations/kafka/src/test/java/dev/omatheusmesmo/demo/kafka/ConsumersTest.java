package dev.omatheusmesmo.demo.kafka;

import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnDuplicatedContext;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnEventLoop;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnVirtualThread;
import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnWorker;
import static io.restassured.RestAssured.delete;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ConsumersTest {

    DevServicesContext devServices;

    @BeforeEach
    void reset() {
        delete("/consumers").then().statusCode(204);
    }

    @Test
    void theDefaultConsumerRunsOnAnEventLoop() {
        Orders.send(devServices, "orders-event-loop", "1:10");

        Orders.awaitProcessed("event-loop", 1)
                .body("processed[0].thread", ranOnEventLoop())
                .body("processed[0].thread", ranOnDuplicatedContext());
    }

    /**
     * Since Quarkus 3.10 (quarkusio/quarkus#39768), Reactive Messaging follows the same signature rule as Quarkus
     * REST: a synchronous consumer with no annotation runs on a worker. Before that, it ran on the event loop, and
     * {@code quarkus.messaging.blocking.signatures.execution.mode=event-loop} brings that behaviour back.
     */
    @Test
    void aSynchronousConsumerWithoutAnnotationsRunsOnAWorker() {
        Orders.send(devServices, "orders-plain", "1:0");

        Orders.awaitProcessed("plain", 1).body("processed[0].thread", ranOnWorker());
    }

    /**
     * Quarkus REST stopped treating {@code @Transactional} as blocking (quarkusio/quarkus#51063). Reactive
     * Messaging still does.
     */
    @Test
    void aTransactionalConsumerRunsOnAWorker() {
        Orders.send(devServices, "orders-transactional", "1:0");

        Orders.awaitProcessed("transactional", 1).body("processed[0].thread", ranOnWorker());
    }

    @Test
    void theBlockingConsumerRunsOnAWorker() {
        Orders.send(devServices, "orders-blocking", "1:10");

        Orders.awaitProcessed("blocking", 1).body("processed[0].thread", ranOnWorker());
    }

    @Test
    void theVirtualThreadConsumerRunsOnAVirtualThread() {
        Orders.send(devServices, "orders-virtual", "1:10");

        Orders.awaitProcessed("virtual", 1).body("processed[0].thread", ranOnVirtualThread());
    }

    @Test
    void theEventLoopConsumerKeepsTheArrivalOrder() {
        Orders.send(devServices, "orders-event-loop", "1:300", "2:200", "3:100");

        Orders.awaitProcessed("event-loop", 3).body("processed.sequence", contains(1, 2, 3)).body("peak", is(1));
    }

    @Test
    void theBlockingConsumerKeepsTheArrivalOrder() {
        Orders.send(devServices, "orders-blocking", "1:300", "2:200", "3:100");

        Orders.awaitProcessed("blocking", 3).body("processed.sequence", contains(1, 2, 3)).body("peak", is(1));
    }

    @Test
    void theVirtualThreadConsumerProcessesConcurrentlyAndLosesTheOrder() {
        Orders.send(devServices, "orders-virtual", "1:300", "2:200", "3:100");

        Orders.awaitProcessed("virtual", 3).body("processed.sequence", contains(3, 2, 1)).body("peak", is(3));
    }
}
