package dev.omatheusmesmo.demo.threading.context;

import dev.omatheusmesmo.demo.common.Observed;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/context")
public class ContextResource {

    @GET
    @Path("/virtual")
    @RunOnVirtualThread
    public Observed<Tags> virtual() {
        return Observed.of(Tags.read());
    }

    @GET
    @Path("/worker")
    public Observed<Tags> worker() {
        return Observed.of(Tags.read());
    }

    @GET
    @Path("/event-loop")
    @NonBlocking
    public Observed<Tags> eventLoop() {
        return Observed.of(Tags.read());
    }

    @GET
    @Path("/hop")
    public Uni<Observed<Tags>> hop() {
        Context context = Vertx.currentContext();
        return Uni.createFrom().completionStage(() -> context
                .executeBlocking(() -> Observed.of(Tags.read()))
                .toCompletionStage());
    }

    /**
     * Runs the store on the event loop's root context instead of the request's duplicated context, through the
     * same {@link ContextLocals} API the filter uses. Only the context differs, so the refusal comes from the
     * root context itself.
     */
    @GET
    @Path("/root")
    public Uni<Observed<String>> root() {
        Context root = VertxContext.getRootContext(Vertx.currentContext());
        return Uni.createFrom()
                .item(() -> {
                    ContextLocals.put(RequestTagFilter.KEY, "shared by every request on this event loop");
                    return Observed.of("stored on the root context");
                })
                .runSubscriptionOn(task -> root.runOnContext(ignored -> task.run()));
    }
}
