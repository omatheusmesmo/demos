package dev.omatheusmesmo.demo.mutiny.threads;

import io.smallrye.mutiny.Multi;
import java.util.concurrent.Executor;

/**
 * Reports which thread processes each item. Stages run on the emitting thread unless {@code emitOn} moves
 * every following stage to another executor.
 */
public final class ItemProcessor {

    public <T> Multi<String> processingThreads(Multi<T> items) {
        return items.map(item -> Thread.currentThread().getName());
    }

    public <T> Multi<String> processingThreadsOn(Multi<T> items, Executor executor) {
        return processingThreads(items.emitOn(executor));
    }
}
