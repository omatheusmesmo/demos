package dev.omatheusmesmo.demo.mutiny.composition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class QuoteAggregatorTest {

    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private final QuoteAggregator aggregator = new QuoteAggregator();

    @AfterEach
    void shutdown() {
        pool.close();
    }

    @Test
    void combiningSynchronousSourcesRunsThemOneAfterTheOther() {
        List<String> timeline = new CopyOnWriteArrayList<>();

        List<String> quotes = aggregator.both(recording("a", timeline), recording("b", timeline))
                .await().atMost(Duration.ofSeconds(5));

        assertEquals(List.of("a", "b"), quotes);
        assertEquals(List.of("a:start", "a:end", "b:start", "b:end"), timeline);
    }

    @Test
    void combiningAsynchronousSourcesRunsThemAtTheSameTime() {
        CountDownLatch bothStarted = new CountDownLatch(2);

        List<String> quotes = aggregator.both(waitingForTheOther("a", bothStarted), waitingForTheOther("b", bothStarted))
                .await().atMost(Duration.ofSeconds(5));

        assertEquals(List.of("a", "b"), quotes);
    }

    private static Uni<String> recording(String name, List<String> timeline) {
        return Uni.createFrom().item(() -> {
            timeline.add(name + ":start");
            timeline.add(name + ":end");
            return name;
        });
    }

    private Uni<String> waitingForTheOther(String name, CountDownLatch bothStarted) {
        return Uni.createFrom().item(() -> {
            bothStarted.countDown();
            return awaitQuietly(bothStarted) ? name : name + ":ran-alone";
        }).runSubscriptionOn(pool);
    }

    private static boolean awaitQuietly(CountDownLatch latch) {
        try {
            return latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
