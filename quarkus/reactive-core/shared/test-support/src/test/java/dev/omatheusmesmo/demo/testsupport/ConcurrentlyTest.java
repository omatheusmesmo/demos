package dev.omatheusmesmo.demo.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConcurrentlyTest {

    private final SlowBackend backend = new SlowBackend();

    @BeforeEach
    void start() {
        backend.init(Map.of(SlowBackend.PROPERTY, "url", SlowBackend.DELAY_MS, "100"));
        RestAssured.baseURI = backend.start().get("url");
    }

    @AfterEach
    void stop() {
        backend.stop();
        RestAssured.reset();
    }

    @Test
    void sendsEveryRequestAndReturnsEveryResponse() {
        List<Response> responses = Concurrently.get(8, "/ping");

        assertEquals(8, responses.size());
        responses.forEach(response -> assertEquals(200, response.statusCode()));
        assertEquals(8, backend.receivedRequests());
    }
}
