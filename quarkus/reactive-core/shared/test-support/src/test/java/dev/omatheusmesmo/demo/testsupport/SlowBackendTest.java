package dev.omatheusmesmo.demo.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SlowBackendTest {

    private final SlowBackend backend = new SlowBackend();

    @AfterEach
    void stop() {
        backend.stop();
    }

    @Test
    void exposesItsUrlUnderTheConfiguredProperty() {
        backend.init(Map.of(SlowBackend.PROPERTY, "upstream.url"));

        Map<String, String> config = backend.start();

        assertTrue(config.get("upstream.url").startsWith("http://localhost:"));
    }

    @Test
    void answersEveryGetWithTheConfiguredBodyAfterTheDelay() throws Exception {
        backend.init(Map.of(
                SlowBackend.PROPERTY, "upstream.url",
                SlowBackend.DELAY_MS, "200",
                SlowBackend.BODY, "{\"value\":\"slow\"}"));
        String url = backend.start().get("upstream.url");

        long started = System.nanoTime();
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url + "/anything")).build(),
                HttpResponse.BodyHandlers.ofString());
        Duration elapsed = Duration.ofNanos(System.nanoTime() - started);

        assertEquals(200, response.statusCode());
        assertEquals("{\"value\":\"slow\"}", response.body());
        assertTrue(elapsed.toMillis() >= 200, "expected at least 200 ms, got " + elapsed.toMillis());
    }
}
