package dev.omatheusmesmo.demo.postgres;

import static dev.omatheusmesmo.demo.testsupport.ThreadMatchers.ranOnVirtualThread;
import static io.restassured.RestAssured.get;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.virtual.ShouldNotPin;
import io.quarkus.test.junit.virtual.VirtualThreadUnit;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * The first blocking socket read from a virtual thread makes the JDK initialise {@code sun.nio.ch.Poller}, and a
 * virtual thread that waits for that class initialisation is pinned once. The warm-up request absorbs that one-off
 * pin, so the assertion measures the steady state of Hibernate ORM and the PostgreSQL driver.
 */
@QuarkusTest
@VirtualThreadUnit
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PinningTest {

    @Test
    @Order(1)
    void warmUp() {
        get("/books/virtual").then().statusCode(200).body("thread", ranOnVirtualThread());
    }

    @Test
    @Order(2)
    @ShouldNotPin
    void hibernateOrmAndThePostgresqlDriverDoNotPinAVirtualThread() {
        get("/books/virtual").then().statusCode(200).body("thread", ranOnVirtualThread());
    }
}
