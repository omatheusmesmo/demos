package dev.omatheusmesmo.demo.threading.blocking;

import dev.omatheusmesmo.demo.common.Observed;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.time.Duration;

/**
 * The same {@code await()} on an event loop, where Quarkus refuses it, and on a virtual thread, where it only
 * parks the virtual thread.
 */
@Path("/await")
public class AwaitResource {

    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    @GET
    @Path("/event-loop")
    @NonBlocking
    public Observed<String> onEventLoop() {
        return Observed.of(slowResult().await().atMost(TIMEOUT));
    }

    @GET
    @Path("/virtual")
    @RunOnVirtualThread
    public Observed<String> onVirtualThread() {
        return Observed.of(slowResult().await().atMost(TIMEOUT));
    }

    private static Uni<String> slowResult() {
        return Uni.createFrom().item("done").onItem().delayIt().by(Duration.ofMillis(20));
    }
}
