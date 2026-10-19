package dev.omatheusmesmo.demo.kafka;

import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnEventLoop;
import static io.restassured.RestAssured.delete;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The switch back to the behaviour before Quarkus 3.10, when a synchronous consumer ran on the event loop.
 */
@QuarkusTest
@TestProfile(EventLoopExecutionModeTest.EventLoopForSynchronousSignatures.class)
class EventLoopExecutionModeTest {

    DevServicesContext devServices;

    @BeforeEach
    void reset() {
        delete("/consumers").then().statusCode(204);
    }

    @Test
    void aSynchronousConsumerRunsOnTheEventLoopWhenAskedTo() {
        Orders.send(devServices, "orders-plain", "1:0");

        Orders.awaitProcessed("plain", 1).body("processed[0].thread", ranOnEventLoop());
    }

    public static class EventLoopForSynchronousSignatures implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.messaging.blocking.signatures.execution.mode", "event-loop");
        }
    }
}
