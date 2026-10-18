package dev.omatheusmesmo.demo.kafka;

import static io.restassured.RestAssured.delete;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Virtual thread consumers run up to 1024 messages at once by default. The messaging guide documents the key
 * {@code smallrye.messaging.worker.<virtual-thread>.max-concurrency}; this test checks that the placeholder is
 * literal.
 */
@QuarkusTest
@TestProfile(ConcurrencyLimitTest.TwoAtATime.class)
class ConcurrencyLimitTest {

    DevServicesContext devServices;

    @Test
    void theVirtualThreadConsumerHonoursTheConfiguredConcurrency() {
        delete("/consumers").then().statusCode(204);

        Orders.send(devServices, "orders-virtual", "1:300", "2:300", "3:300", "4:300", "5:300", "6:300");

        Orders.awaitProcessed("virtual", 6).body("peak", is(2));
    }

    public static class TwoAtATime implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("smallrye.messaging.worker.<virtual-thread>.max-concurrency", "2");
        }
    }
}
