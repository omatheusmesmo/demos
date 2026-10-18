package dev.omatheusmesmo.demo.restclient;

import static io.restassured.RestAssured.get;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.virtual.ShouldNotPin;
import io.quarkus.test.junit.virtual.VirtualThreadUnit;
import org.junit.jupiter.api.Test;

@QuarkusTest
@SlowUpstream
@VirtualThreadUnit
class PinningTest {

    @Test
    @ShouldNotPin
    void theRestClientDoesNotPinAVirtualThread() {
        get("/quotes/virtual").then().statusCode(200);
    }
}
