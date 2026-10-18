package dev.omatheusmesmo.demo.mutiny.threads;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.smallrye.mutiny.Multi;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ItemProcessorTest {

    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> new Thread(task, "processor-worker"));
    private final ItemProcessor processor = new ItemProcessor();

    @AfterEach
    void shutdown() {
        worker.close();
    }

    @Test
    void stagesRunOnTheThreadThatEmittedTheItem() {
        List<String> threads = processor.processingThreads(Multi.createFrom().items(1, 2, 3))
                .collect().asList().await().atMost(Duration.ofSeconds(5));

        String caller = Thread.currentThread().getName();
        assertEquals(List.of(caller, caller, caller), threads);
    }

    @Test
    void emitOnMovesEveryFollowingStageToTheExecutor() {
        List<String> threads = processor.processingThreadsOn(Multi.createFrom().items(1, 2, 3), worker)
                .collect().asList().await().atMost(Duration.ofSeconds(5));

        assertEquals(List.of("processor-worker", "processor-worker", "processor-worker"), threads);
    }
}
