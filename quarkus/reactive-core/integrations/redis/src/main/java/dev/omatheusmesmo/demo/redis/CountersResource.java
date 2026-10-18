package dev.omatheusmesmo.demo.redis;

import dev.omatheusmesmo.demo.common.Observed;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * Increments a counter per model. The blocking client is a thin layer over the reactive one, which is why it is
 * safe on a virtual thread: it waits on a Vert.x connection instead of a JDK socket.
 */
@Path("/counters")
public class CountersResource {

    private final ValueCommands<String, Long> blocking;
    private final ReactiveValueCommands<String, Long> reactive;

    public CountersResource(RedisDataSource redis, ReactiveRedisDataSource reactiveRedis) {
        this.blocking = redis.value(Long.class);
        this.reactive = reactiveRedis.value(Long.class);
    }

    @GET
    @Path("/worker")
    public Observed<Long> worker() {
        return Observed.of(blocking.incr("visits:worker"));
    }

    @GET
    @Path("/reactive")
    public Uni<Observed<Long>> reactive() {
        return reactive.incr("visits:reactive").map(Observed::of);
    }

    @GET
    @Path("/virtual")
    @RunOnVirtualThread
    public Observed<Long> virtual() {
        return Observed.of(blocking.incr("visits:virtual"));
    }
}
