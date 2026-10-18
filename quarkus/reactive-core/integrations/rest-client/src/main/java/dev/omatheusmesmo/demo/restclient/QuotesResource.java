package dev.omatheusmesmo.demo.restclient;

import dev.omatheusmesmo.demo.common.ConcurrencyTracker;
import dev.omatheusmesmo.demo.common.Observed;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.time.Duration;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Fetches two quotes from the slow upstream in each model. The worker and the virtual thread call it twice in a
 * row; the reactive variant combines both calls so they overlap.
 */
@Path("/quotes")
public class QuotesResource {

    static final Duration TIMEOUT = Duration.ofSeconds(1);

    private final UpstreamClient upstream;
    private final ConcurrencyTracker inFlight;

    public QuotesResource(@RestClient UpstreamClient upstream, ConcurrencyTracker inFlight) {
        this.upstream = upstream;
        this.inFlight = inFlight;
    }

    @GET
    @Path("/worker")
    public Observed<List<Quote>> worker() {
        return inFlight.track(() -> Observed.of(List.of(upstream.quote(), upstream.quote())));
    }

    @GET
    @Path("/reactive")
    public Uni<Observed<List<Quote>>> reactive() {
        return inFlight.track(Uni.combine().all()
                .unis(upstream.quoteAsync(), upstream.quoteAsync())
                .with((first, second) -> List.of(first, second))
                .ifNoItem().after(TIMEOUT).fail()
                .map(Observed::of));
    }

    @GET
    @Path("/virtual")
    @RunOnVirtualThread
    public Observed<List<Quote>> virtual() {
        return inFlight.track(() -> Observed.of(List.of(
                upstream.quoteAsync().await().atMost(TIMEOUT),
                upstream.quoteAsync().await().atMost(TIMEOUT))));
    }

    @GET
    @Path("/peak")
    public int peak() {
        return inFlight.peak();
    }

    @DELETE
    @Path("/peak")
    public void resetPeak() {
        inFlight.reset();
    }
}
