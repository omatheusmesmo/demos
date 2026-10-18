package dev.omatheusmesmo.demo.mutiny.streams;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PageFetcherTest {

    private final AtomicInteger pagesFetched = new AtomicInteger();
    private final PageFetcher fetcher = new PageFetcher();

    @Test
    void fetchesPagesUntilOneComesBackEmpty() {
        List<List<Integer>> pages = List.of(List.of(1, 2), List.of(3, 4), List.of());

        List<Integer> items = fetcher.stream(page -> {
                    pagesFetched.incrementAndGet();
                    return Uni.createFrom().item(pages.get(page - 1));
                })
                .collect().asList().await().atMost(Duration.ofSeconds(5));

        assertEquals(List.of(1, 2, 3, 4), items);
        assertEquals(3, pagesFetched.get());
    }

    @Test
    void stopsFetchingOnceTheConsumerHasEnough() {
        List<Integer> items = fetcher.<Integer>stream(page -> {
                    pagesFetched.incrementAndGet();
                    return Uni.createFrom().item(List.of(page * 2 - 1, page * 2));
                })
                .select().first(3)
                .collect().asList().await().atMost(Duration.ofSeconds(5));

        assertEquals(List.of(1, 2, 3), items);
        assertEquals(2, pagesFetched.get());
    }
}
