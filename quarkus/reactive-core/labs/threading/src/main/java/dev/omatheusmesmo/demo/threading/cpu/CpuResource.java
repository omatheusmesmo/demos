package dev.omatheusmesmo.demo.threading.cpu;

import dev.omatheusmesmo.demo.common.Observed;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * The same CPU-bound work in the three models. The reactive variant computes on the event loop on purpose, to
 * show what CPU work costs there.
 */
@Path("/cpu")
public class CpuResource {

    public static final int ROUNDS = 20_000;

    @GET
    @Path("/worker")
    public Observed<String> worker() {
        return Observed.of(Hashing.digest(ROUNDS));
    }

    @GET
    @Path("/reactive")
    public Uni<Observed<String>> reactive() {
        return Uni.createFrom().item(() -> Observed.of(Hashing.digest(ROUNDS)));
    }

    @GET
    @Path("/virtual")
    @RunOnVirtualThread
    public Observed<String> virtual() {
        return Observed.of(Hashing.digest(ROUNDS));
    }
}
