package dev.omatheusmesmo.demo.restclient;

import dev.omatheusmesmo.demo.common.Observed;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.time.Duration;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * The endpoints the benchmark measures. Every model does the same work: both upstream calls start at once through
 * the reactive client, and the pair has one deadline. The only difference is what waits for the result: a worker
 * thread that blocks, the event loop that does not wait at all, or a virtual thread that parks.
 *
 * <p>{@link QuotesResource} keeps the shape each model would naturally take (calls in a row, each with its own
 * timeout mechanism), which is what the labs teach but would make a benchmark compare code instead of models.
 */
@Path("/fan-out")
public class FanOutResource {

    static final Duration DEADLINE = Duration.ofSeconds(1);

    private final UpstreamClient upstream;

    public FanOutResource(@RestClient UpstreamClient upstream) {
        this.upstream = upstream;
    }

    @GET
    @Path("/worker")
    public Observed<List<Quote>> worker() {
        return Observed.of(bothQuotes().await().atMost(DEADLINE));
    }

    @GET
    @Path("/reactive")
    public Uni<Observed<List<Quote>>> reactive() {
        return bothQuotes().ifNoItem().after(DEADLINE).fail().map(Observed::of);
    }

    @GET
    @Path("/virtual")
    @RunOnVirtualThread
    public Observed<List<Quote>> virtual() {
        return Observed.of(bothQuotes().await().atMost(DEADLINE));
    }

    private Uni<List<Quote>> bothQuotes() {
        return Uni.combine().all()
                .unis(upstream.quoteAsync(), upstream.quoteAsync())
                .with((first, second) -> List.of(first, second));
    }
}
