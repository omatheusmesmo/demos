package dev.omatheusmesmo.demo.threading.pinning;

import static io.restassured.RestAssured.get;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.virtual.ShouldNotPin;
import io.quarkus.test.junit.virtual.ShouldPin;
import io.quarkus.test.junit.virtual.VirtualThreadUnit;
import org.junit.jupiter.api.Test;

@QuarkusTest
@VirtualThreadUnit
class PinningTest {

    @Test
    @ShouldNotPin
    void aPlainVirtualThreadEndpointDoesNotPin() {
        get("/dispatch/virtual").then().statusCode(200);
    }

    @Test
    @ShouldPin(atMost = 1)
    void aBlockingStaticInitializerPinsTheCarrier() {
        get("/pinning/class-initializer").then().statusCode(200);
    }
}
