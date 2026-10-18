package dev.omatheusmesmo.demo.kafka;

import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnVirtualThread;
import static io.restassured.RestAssured.delete;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.virtual.ShouldNotPin;
import io.quarkus.test.junit.virtual.VirtualThreadUnit;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@VirtualThreadUnit
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PinningTest {

    DevServicesContext devServices;

    @Test
    @Order(1)
    void warmUp() {
        consumeOneOrder("1:10");
    }

    @Test
    @Order(2)
    @ShouldNotPin
    void aVirtualThreadConsumerDoesNotPin() {
        consumeOneOrder("2:10");
    }

    private void consumeOneOrder(String payload) {
        delete("/consumers").then().statusCode(204);
        Orders.send(devServices, "orders-virtual", payload);
        Orders.awaitProcessed("virtual", 1).body("processed[0].thread", ranOnVirtualThread());
    }
}
