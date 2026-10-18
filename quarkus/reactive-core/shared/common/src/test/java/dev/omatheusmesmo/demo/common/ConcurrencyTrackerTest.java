package dev.omatheusmesmo.demo.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.smallrye.mutiny.Uni;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ConcurrencyTrackerTest {

    private final ConcurrencyTracker tracker = new ConcurrencyTracker();

    @Test
    void peakIsZeroBeforeAnyWork() {
        assertEquals(0, tracker.peak());
        assertEquals(0, tracker.inFlight());
    }

    @Test
    void recordsThePeakOfOverlappingWork() throws InterruptedException {
        int tasks = 5;
        CountDownLatch allStarted = new CountDownLatch(tasks);
        CountDownLatch release = new CountDownLatch(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                executor.submit(() -> tracker.track(() -> {
                    allStarted.countDown();
                    await(release);
                    return "done";
                }));
            }
            allStarted.await(5, TimeUnit.SECONDS);
            assertEquals(tasks, tracker.inFlight());
            release.countDown();
        }

        assertEquals(tasks, tracker.peak());
        assertEquals(0, tracker.inFlight());
    }

    @Test
    void sequentialWorkNeverOverlaps() {
        tracker.track(() -> "first");
        tracker.track(() -> "second");

        assertEquals(1, tracker.peak());
    }

    @Test
    void releasesTheSlotWhenWorkFails() {
        try {
            tracker.track(() -> {
                throw new IllegalStateException("boom");
            });
        } catch (IllegalStateException expected) {
            assertEquals("boom", expected.getMessage());
        }

        assertEquals(0, tracker.inFlight());
    }

    @Test
    void countsAUniAsInFlightUntilItCompletes() {
        CompletableFuture<String> pending = new CompletableFuture<>();
        Uni<String> tracked = tracker.track(Uni.createFrom().completionStage(pending));

        tracked.subscribe().with(item -> { });
        assertEquals(1, tracker.inFlight());

        pending.complete("done");
        assertEquals(0, tracker.inFlight());
        assertEquals(1, tracker.peak());
    }

    @Test
    void doesNotCountAUniThatWasNeverSubscribed() {
        tracker.track(Uni.createFrom().item("lazy"));

        assertEquals(0, tracker.peak());
    }

    @Test
    void resetClearsThePeak() {
        tracker.track(() -> "work");
        tracker.reset();

        assertEquals(0, tracker.peak());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
