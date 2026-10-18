package dev.omatheusmesmo.demo.mutiny.composition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class OrderedFanOutTest {

    private static final Function<Integer, Uni<Integer>> SLOWER_FOR_SMALLER_NUMBERS =
            n -> Uni.createFrom().item(n).onItem().delayIt().by(Duration.ofMillis((4 - n) * 150L));

    private final OrderedFanOut fanOut = new OrderedFanOut();

    @Test
    void concatenatePreservesTheInputOrder() {
        List<Integer> results = fanOut.concatenate(Multi.createFrom().items(1, 2, 3), SLOWER_FOR_SMALLER_NUMBERS)
                .collect().asList().await().atMost(Duration.ofSeconds(5));

        assertEquals(List.of(1, 2, 3), results);
    }

    @Test
    void mergeEmitsResultsAsSoonAsTheyComplete() {
        List<Integer> results = fanOut.merge(Multi.createFrom().items(1, 2, 3), SLOWER_FOR_SMALLER_NUMBERS)
                .collect().asList().await().atMost(Duration.ofSeconds(5));

        assertEquals(List.of(3, 2, 1), results);
    }
}
