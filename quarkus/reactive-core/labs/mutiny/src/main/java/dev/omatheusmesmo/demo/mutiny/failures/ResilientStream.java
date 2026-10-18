package dev.omatheusmesmo.demo.mutiny.failures;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.util.function.Function;

/**
 * A failure is a terminal event: recovering at the end of a stream emits the fallback and stops. Isolating each
 * item in its own {@link Uni} confines the failure to that item and lets the stream continue.
 */
public final class ResilientStream {

    public <T, R> Multi<R> recoverAtTheEnd(Multi<T> items, Function<T, R> mapper, R fallback) {
        return items.map(mapper).onFailure().recoverWithItem(fallback);
    }

    public <T, R> Multi<R> isolatePerItem(Multi<T> items, Function<T, R> mapper, R fallback) {
        return items.onItem().transformToUniAndConcatenate(item -> Uni.createFrom().item(() -> mapper.apply(item))
                .onFailure().recoverWithItem(fallback));
    }
}
