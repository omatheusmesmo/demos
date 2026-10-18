package dev.omatheusmesmo.demo.threading.pinning;

import dev.omatheusmesmo.demo.common.Observed;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/pinning")
public class PinningResource {

    @GET
    @Path("/class-initializer")
    @RunOnVirtualThread
    public Observed<String> classInitializer() {
        return Observed.of(SlowInitializer.VALUE);
    }
}
