package dev.omatheusmesmo.demo.mutiny.composition;

import io.smallrye.mutiny.Uni;
import java.util.List;

/**
 * Subscribes to both sources and joins their items. The sources only overlap if they are asynchronous:
 * {@code combine} subscribes to each one in turn and does not schedule them on other threads.
 */
public final class QuoteAggregator {

    public Uni<List<String>> both(Uni<String> first, Uni<String> second) {
        return Uni.combine().all().unis(first, second).with((a, b) -> List.of(a, b));
    }
}
