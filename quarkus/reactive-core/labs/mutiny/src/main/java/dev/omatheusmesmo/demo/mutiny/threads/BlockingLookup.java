package dev.omatheusmesmo.demo.mutiny.threads;

import io.smallrye.mutiny.Uni;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Wraps a blocking call in a {@link Uni}. Mutiny schedules nothing on its own: without
 * {@code runSubscriptionOn}, the blocking call runs on whichever thread subscribes.
 */
public final class BlockingLookup {

    private final Function<String, String> blockingCall;

    public BlockingLookup(Function<String, String> blockingCall) {
        this.blockingCall = blockingCall;
    }

    public Uni<String> inline(String key) {
        return Uni.createFrom().item(() -> blockingCall.apply(key));
    }

    public Uni<String> offloaded(String key, Executor executor) {
        return inline(key).runSubscriptionOn(executor);
    }
}
