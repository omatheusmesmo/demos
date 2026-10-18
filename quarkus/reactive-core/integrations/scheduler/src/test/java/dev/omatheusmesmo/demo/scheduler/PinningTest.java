package dev.omatheusmesmo.demo.scheduler;

import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnVirtualThread;

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

    @Test
    @Order(1)
    void warmUp() {
        Jobs.awaitExecutions("virtual", 1);
    }

    @Test
    @Order(2)
    @ShouldNotPin
    void aVirtualThreadJobDoesNotPin() {
        int seen = Jobs.executions("virtual");
        Jobs.awaitExecutions("virtual", seen + 1).body("lastThread", ranOnVirtualThread());
    }
}
