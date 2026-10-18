package dev.omatheusmesmo.demo.mutiny.composition;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.util.function.Function;

/**
 * Calls an asynchronous operation per item. {@code concatenate} keeps the input order and waits for each
 * call; {@code merge} runs the calls concurrently and emits results in completion order.
 */
public final class OrderedFanOut {

    public <T, R> Multi<R> concatenate(Multi<T> items, Function<? super T, Uni<R>> call) {
        return items.onItem().transformToUniAndConcatenate(call::apply);
    }

    public <T, R> Multi<R> merge(Multi<T> items, Function<? super T, Uni<R>> call) {
        return items.onItem().transformToUniAndMerge(call::apply);
    }
}
