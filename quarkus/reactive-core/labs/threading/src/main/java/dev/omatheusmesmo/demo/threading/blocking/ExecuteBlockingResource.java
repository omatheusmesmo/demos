package dev.omatheusmesmo.demo.threading.blocking;

import dev.omatheusmesmo.demo.common.ConcurrencyTracker;
import dev.omatheusmesmo.demo.common.Observed;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * Submits two blocking tasks from the same Vert.x context and reports how many ran at the same time.
 */
@Path("/execute-blocking")
public class ExecuteBlockingResource {

    private static final long TASK_MILLIS = 200;

    private final Vertx vertx;

    public ExecuteBlockingResource(Vertx vertx) {
        this.vertx = vertx;
    }

    @GET
    @Path("/ordered")
    public Uni<Observed<Integer>> ordered() {
        return peakOfTwoTasks(true);
    }

    @GET
    @Path("/unordered")
    public Uni<Observed<Integer>> unordered() {
        return peakOfTwoTasks(false);
    }

    private Uni<Observed<Integer>> peakOfTwoTasks(boolean ordered) {
        ConcurrencyTracker tracker = new ConcurrencyTracker();
        Context context = vertx.getOrCreateContext();
        return Uni.combine().all()
                .unis(submit(context, tracker, ordered), submit(context, tracker, ordered))
                .with((first, second) -> Observed.of(tracker.peak()));
    }

    private static Uni<String> submit(Context context, ConcurrencyTracker tracker, boolean ordered) {
        return Uni.createFrom().completionStage(() -> context
                .executeBlocking(() -> tracker.track(ExecuteBlockingResource::blockForAWhile), ordered)
                .toCompletionStage());
    }

    private static String blockForAWhile() {
        try {
            Thread.sleep(TASK_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "done";
    }
}
