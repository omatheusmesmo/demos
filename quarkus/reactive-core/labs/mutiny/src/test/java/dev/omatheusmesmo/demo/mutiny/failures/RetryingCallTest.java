package dev.omatheusmesmo.demo.mutiny.failures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RetryingCallTest {

    private final AtomicInteger subscriptions = new AtomicInteger();
    private final RetryingCall retrying = new RetryingCall(2);

    @Test
    void retryingIsResubscribingToTheUpstream() {
        Uni<String> flaky = Uni.createFrom().item(() -> {
            if (subscriptions.incrementAndGet() < 3) {
                throw new IllegalStateException("flaky");
            }
            return "ok";
        });

        String result = retrying.call(flaky).await().atMost(Duration.ofSeconds(5));

        assertEquals("ok", result);
        assertEquals(3, subscriptions.get());
    }

    @Test
    void givesUpWithTheLastFailureWhenEveryAttemptFails() {
        Uni<String> broken = Uni.createFrom().item(() -> {
            throw new IllegalStateException("failure-" + subscriptions.incrementAndGet());
        });

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> retrying.call(broken).await().atMost(Duration.ofSeconds(5)));

        assertEquals("failure-3", failure.getMessage());
        assertEquals(3, subscriptions.get());
    }
}
