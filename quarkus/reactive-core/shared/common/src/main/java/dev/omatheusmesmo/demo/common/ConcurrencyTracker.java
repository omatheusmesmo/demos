package dev.omatheusmesmo.demo.common;

import io.smallrye.mutiny.Uni;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Counts work in flight and remembers the highest value reached.
 *
 * <p>Synchronous work is in flight while the supplier runs. A {@link Uni} is in flight from the moment it is
 * subscribed until it emits an item, a failure or is cancelled, so an unsubscribed {@code Uni} is never counted.
 */
public final class ConcurrencyTracker {

    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicInteger peak = new AtomicInteger();

    public <T> T track(Supplier<T> work) {
        enter();
        try {
            return work.get();
        } finally {
            exit();
        }
    }

    public <T> Uni<T> track(Uni<T> work) {
        return Uni.createFrom().deferred(() -> {
            enter();
            return work.onTermination().invoke(this::exit);
        });
    }

    public int inFlight() {
        return inFlight.get();
    }

    public int peak() {
        return peak.get();
    }

    public void reset() {
        peak.set(inFlight.get());
    }

    private void enter() {
        int current = inFlight.incrementAndGet();
        peak.accumulateAndGet(current, Math::max);
    }

    private void exit() {
        inFlight.decrementAndGet();
    }
}
