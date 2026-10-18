package dev.omatheusmesmo.demo.kafka;

import dev.omatheusmesmo.demo.common.ConcurrencyTracker;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * What one consumer has done: the orders it finished, in completion order, and how many it processed at once.
 */
public final class ConsumerLog {

    private final List<Processed> processed = new CopyOnWriteArrayList<>();
    private final ConcurrencyTracker inFlight = new ConcurrencyTracker();

    ConcurrencyTracker inFlight() {
        return inFlight;
    }

    void finished(Processed order) {
        processed.add(order);
    }

    ConsumerReport report() {
        return new ConsumerReport(List.copyOf(processed), inFlight.peak());
    }

    void clear() {
        processed.clear();
        inFlight.reset();
    }
}
