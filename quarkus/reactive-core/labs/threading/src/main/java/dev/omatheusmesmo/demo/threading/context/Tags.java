package dev.omatheusmesmo.demo.threading.context;

import io.smallrye.common.vertx.ContextLocals;

/**
 * The request tag as seen through each propagation mechanism, plus the thread the request filter ran on.
 */
public record Tags(String contextLocal, String threadLocal, String filterThread) {

    static Tags read() {
        return new Tags(
                ContextLocals.<String>get(RequestTagFilter.KEY).orElse(null),
                RequestTagFilter.THREAD_TAG.get(),
                ContextLocals.<String>get(RequestTagFilter.FILTER_THREAD).orElse(null));
    }
}
