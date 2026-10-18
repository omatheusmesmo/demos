package dev.omatheusmesmo.demo.common;

import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;

/**
 * Snapshot of the thread that is running the current code, as seen by Vert.x.
 *
 * @param name              the thread name, empty for unnamed virtual threads
 * @param virtual           whether the thread is a virtual thread
 * @param eventLoop         whether the thread is a Vert.x event loop
 * @param duplicatedContext whether the code runs on a Vert.x duplicated context
 */
public record ThreadReport(String name, boolean virtual, boolean eventLoop, boolean duplicatedContext) {

    public static ThreadReport capture() {
        Thread current = Thread.currentThread();
        return new ThreadReport(
                current.getName(),
                current.isVirtual(),
                Context.isOnEventLoopThread(),
                VertxContext.isOnDuplicatedContext());
    }
}
