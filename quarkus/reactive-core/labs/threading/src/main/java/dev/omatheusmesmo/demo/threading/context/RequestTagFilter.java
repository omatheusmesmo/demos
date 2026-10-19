package dev.omatheusmesmo.demo.threading.context;

import io.smallrye.common.vertx.ContextLocals;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * Stores the {@value #HEADER} header twice, in the duplicated context and in a {@link ThreadLocal}, and records
 * the thread the filter ran on, so the endpoints can show which mechanism reaches the code serving the request.
 *
 * <p>It does so from two filters: a regular one, which runs on the thread of the matched endpoint, and a
 * pre-matching one, which runs on the event loop before Quarkus knows which endpoint, and so which thread, comes next.
 */
public class RequestTagFilter {

    public static final String HEADER = "X-Request-Tag";
    static final Slot REGULAR = new Slot("request-tag", "filter-thread", new ThreadLocal<>());
    static final Slot PRE_MATCHING = new Slot("pre-matching-request-tag", "pre-matching-filter-thread",
            new ThreadLocal<>());

    @ServerRequestFilter
    public void tag(ContainerRequestContext request) {
        REGULAR.store(request.getHeaderString(HEADER));
    }

    @ServerRequestFilter(preMatching = true)
    public void tagBeforeMatching(ContainerRequestContext request) {
        PRE_MATCHING.store(request.getHeaderString(HEADER));
    }

    /**
     * Where one filter keeps the tag: two context local keys and a thread local.
     */
    record Slot(String tagKey, String threadKey, ThreadLocal<String> threadTag) {

        void store(String tag) {
            if (tag == null) {
                threadTag.remove();
                return;
            }
            ContextLocals.put(tagKey, tag);
            ContextLocals.put(threadKey, Thread.currentThread().getName());
            threadTag.set(tag);
        }

        Tags read() {
            return new Tags(
                    ContextLocals.<String>get(tagKey).orElse(null),
                    threadTag.get(),
                    ContextLocals.<String>get(threadKey).orElse(null));
        }
    }
}
