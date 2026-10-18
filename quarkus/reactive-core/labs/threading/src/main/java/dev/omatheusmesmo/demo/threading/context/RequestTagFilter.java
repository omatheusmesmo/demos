package dev.omatheusmesmo.demo.threading.context;

import io.smallrye.common.vertx.ContextLocals;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * Stores the {@value #HEADER} header twice, in the duplicated context and in a {@link ThreadLocal}, and records
 * the thread the filter ran on, so the endpoints can show which mechanism reaches the code serving the request.
 */
public class RequestTagFilter {

    public static final String HEADER = "X-Request-Tag";
    static final String KEY = "request-tag";
    static final String FILTER_THREAD = "filter-thread";
    static final ThreadLocal<String> THREAD_TAG = new ThreadLocal<>();

    @ServerRequestFilter
    public void tag(ContainerRequestContext request) {
        String tag = request.getHeaderString(HEADER);
        if (tag == null) {
            THREAD_TAG.remove();
            return;
        }
        ContextLocals.put(KEY, tag);
        ContextLocals.put(FILTER_THREAD, Thread.currentThread().getName());
        THREAD_TAG.set(tag);
    }
}
