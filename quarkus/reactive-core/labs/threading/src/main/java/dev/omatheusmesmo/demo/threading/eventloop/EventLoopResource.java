package dev.omatheusmesmo.demo.threading.eventloop;

import dev.omatheusmesmo.demo.common.Observed;
import io.smallrye.common.annotation.NonBlocking;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/event-loops")
public class EventLoopResource {

    @GET
    @Path("/current")
    @NonBlocking
    public Observed<String> current() {
        return Observed.of("served by an event loop");
    }
}
