package dev.omatheusmesmo.demo.mutiny.threads;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BlockingLookupTest {

    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> new Thread(task, "lookup-worker"));
    private final BlockingLookup lookup = new BlockingLookup(key -> Thread.currentThread().getName());

    @AfterEach
    void shutdown() {
        worker.close();
    }

    @Test
    void withoutAnOperatorTheBlockingCallRunsOnTheSubscribingThread() {
        String thread = lookup.inline("key").await().atMost(Duration.ofSeconds(5));

        assertEquals(Thread.currentThread().getName(), thread);
    }

    @Test
    void runSubscriptionOnMovesTheBlockingCallToTheExecutor() {
        String thread = lookup.offloaded("key", worker).await().atMost(Duration.ofSeconds(5));

        assertEquals("lookup-worker", thread);
    }
}
