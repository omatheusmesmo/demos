package dev.omatheusmesmo.demo.threading.dispatch;

import dev.omatheusmesmo.demo.common.Observed;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * One endpoint per Quarkus REST dispatch rule. Each returns the thread that ran it.
 */
@Path("/dispatch")
public class DispatchResource {

    @GET
    @Path("/sync")
    public Observed<String> sync() {
        return Observed.of("synchronous signature");
    }

    @GET
    @Path("/uni")
    public Uni<Observed<String>> uni() {
        return Uni.createFrom().item(() -> Observed.of("Uni signature"));
    }

    @GET
    @Path("/transactional-uni")
    @Transactional
    public Uni<Observed<String>> transactionalUni() {
        return Uni.createFrom().item(() -> Observed.of("@Transactional with a Uni signature"));
    }

    @GET
    @Path("/blocking-uni")
    @Blocking
    public Uni<Observed<String>> blockingUni() {
        return Uni.createFrom().item(() -> Observed.of("@Blocking with a Uni signature"));
    }

    @GET
    @Path("/non-blocking")
    @NonBlocking
    public Observed<String> nonBlocking() {
        return Observed.of("@NonBlocking with a synchronous signature");
    }

    @GET
    @Path("/virtual")
    @RunOnVirtualThread
    public Observed<String> virtual() {
        return Observed.of("@RunOnVirtualThread");
    }
}
