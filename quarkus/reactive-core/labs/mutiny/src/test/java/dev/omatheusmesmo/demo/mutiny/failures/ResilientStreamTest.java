package dev.omatheusmesmo.demo.mutiny.failures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.smallrye.mutiny.Multi;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ResilientStreamTest {

    private static final int FALLBACK = -1;
    private static final Function<Integer, Integer> REJECT_SEVEN = n -> {
        if (n == 7) {
            throw new IllegalArgumentException("We don't like seven!");
        }
        return n;
    };

    private final ResilientStream stream = new ResilientStream();

    @Test
    void aFailureIsTerminalEvenWhenTheStreamRecovers() {
        List<Integer> items = stream.recoverAtTheEnd(Multi.createFrom().range(0, 10), REJECT_SEVEN, FALLBACK)
                .collect().asList().await().atMost(Duration.ofSeconds(5));

        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, FALLBACK), items);
    }

    @Test
    void isolatingEachItemKeepsTheStreamAlive() {
        List<Integer> items = stream.isolatePerItem(Multi.createFrom().range(0, 10), REJECT_SEVEN, FALLBACK)
                .collect().asList().await().atMost(Duration.ofSeconds(5));

        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, FALLBACK, 8, 9), items);
    }
}
