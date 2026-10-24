package dev.omatheusmesmo.demo.testsupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OpenLoadTest {

    private final SlowBackend backend = new SlowBackend();

    @AfterEach
    void stop() {
        backend.stop();
    }

    @Test
    void sendsOnScheduleWhileEarlierRequestsAreStillWaiting() {
        URI ping = start(500);

        long begin = System.nanoTime();
        List<Integer> statuses = OpenLoad.get(ping, 20, 10, Duration.ofSeconds(5));
        Duration elapsed = Duration.ofNanos(System.nanoTime() - begin);

        assertThat(statuses, everyItem(is(200)));
        assertThat(elapsed, lessThan(Duration.ofMillis(1500)));
        assertEquals(10, backend.receivedRequests());
    }

    @Test
    void recordsARequestThatTimesOutAsNoResponse() {
        URI ping = start(1000);

        assertEquals(List.of(OpenLoad.NO_RESPONSE), OpenLoad.get(ping, 10, 1, Duration.ofMillis(200)));
    }

    private URI start(int delayMs) {
        backend.init(Map.of(SlowBackend.PROPERTY, "url", SlowBackend.DELAY_MS, String.valueOf(delayMs)));
        return URI.create(backend.start().get("url") + "/ping");
    }
}
