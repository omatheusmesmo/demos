package dev.omatheusmesmo.demo.kafka;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/consumers")
public class ConsumersResource {

    private final ConsumerLogs logs;

    public ConsumersResource(ConsumerLogs logs) {
        this.logs = logs;
    }

    @GET
    @Path("/event-loop")
    public ConsumerReport eventLoop() {
        return logs.eventLoop().report();
    }

    @GET
    @Path("/blocking")
    public ConsumerReport blocking() {
        return logs.blocking().report();
    }

    @GET
    @Path("/virtual")
    public ConsumerReport virtual() {
        return logs.virtual().report();
    }

    @GET
    @Path("/plain")
    public ConsumerReport plain() {
        return logs.plain().report();
    }

    @GET
    @Path("/transactional")
    public ConsumerReport transactional() {
        return logs.transactional().report();
    }

    @DELETE
    public void clear() {
        logs.clear();
    }
}
