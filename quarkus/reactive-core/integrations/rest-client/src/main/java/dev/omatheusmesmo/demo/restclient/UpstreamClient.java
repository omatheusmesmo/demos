package dev.omatheusmesmo.demo.restclient;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * The slow HTTP service. The same call is exposed twice: blocking, for workers, and as a {@link Uni}, for the
 * event loop and for virtual threads that {@code await()} it.
 */
@RegisterRestClient(configKey = "upstream")
@Path("/quote")
public interface UpstreamClient {

    @GET
    Quote quote();

    @GET
    Uni<Quote> quoteAsync();
}
