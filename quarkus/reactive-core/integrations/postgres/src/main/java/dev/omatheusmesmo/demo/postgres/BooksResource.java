package dev.omatheusmesmo.demo.postgres;

import dev.omatheusmesmo.demo.common.ConcurrencyTracker;
import dev.omatheusmesmo.demo.common.Observed;
import io.agroal.api.AgroalDataSource;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.List;

/**
 * The same slow query in each model: Hibernate ORM on a worker and on a virtual thread, Hibernate Reactive on the
 * event loop. {@code /pool} reports the most JDBC connections used at once.
 */
@Path("/books")
public class BooksResource {

    static final String[] TITLES = {"Effective Java", "Java Concurrency in Practice", "Reactive Systems in Java"};

    private final BlockingBooks blockingBooks;
    private final ReactiveBooks reactiveBooks;
    private final ConcurrencyTracker inFlight;
    private final AgroalDataSource dataSource;

    public BooksResource(BlockingBooks blockingBooks, ReactiveBooks reactiveBooks, ConcurrencyTracker inFlight,
            AgroalDataSource dataSource) {
        this.blockingBooks = blockingBooks;
        this.reactiveBooks = reactiveBooks;
        this.inFlight = inFlight;
        this.dataSource = dataSource;
    }

    @GET
    @Path("/worker")
    public Observed<List<String>> worker() {
        return inFlight.track(() -> Observed.of(blockingBooks.slowTitles()));
    }

    @GET
    @Path("/reactive")
    @WithSession
    public Uni<Observed<List<String>>> reactive() {
        return inFlight.track(reactiveBooks.slowTitles().map(Observed::of));
    }

    @GET
    @Path("/virtual")
    @RunOnVirtualThread
    public Observed<List<String>> virtual() {
        return inFlight.track(() -> Observed.of(blockingBooks.slowTitles()));
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

    @GET
    @Path("/pool")
    public long connectionsUsedAtOnce() {
        return dataSource.getMetrics().maxUsedCount();
    }

    @DELETE
    @Path("/pool")
    public void resetPool() {
        dataSource.getMetrics().reset();
    }
}
