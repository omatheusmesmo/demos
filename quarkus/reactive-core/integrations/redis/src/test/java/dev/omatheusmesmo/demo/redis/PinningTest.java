package dev.omatheusmesmo.demo.redis;

import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnVirtualThread;
import static io.restassured.RestAssured.get;

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
        get("/counters/virtual").then().statusCode(200).body("thread", ranOnVirtualThread());
    }

    @Test
    @Order(2)
    @ShouldNotPin
    void theBlockingRedisClientDoesNotPinAVirtualThread() {
        get("/counters/virtual").then().statusCode(200).body("thread", ranOnVirtualThread());
    }
}
