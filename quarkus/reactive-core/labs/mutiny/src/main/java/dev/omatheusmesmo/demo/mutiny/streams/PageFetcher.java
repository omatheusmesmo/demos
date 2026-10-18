package dev.omatheusmesmo.demo.mutiny.streams;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

/**
 * Streams the items of a paginated API, loading page 1, 2, 3 and so on until a page comes back empty. Pages are
 * loaded on demand, so a consumer that cancels early stops the loading.
 */
public final class PageFetcher {

    public <T> Multi<T> stream(IntFunction<Uni<List<T>>> loadPage) {
        return Multi.createBy().repeating()
                .uni(AtomicInteger::new, page -> loadPage.apply(page.incrementAndGet()))
                .until(List::isEmpty)
                .onItem().disjoint();
    }
}
