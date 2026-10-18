package dev.omatheusmesmo.demo.scheduler;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/jobs")
public class JobsResource {

    private final ScheduledJobs jobs;

    public JobsResource(ScheduledJobs jobs) {
        this.jobs = jobs;
    }

    @GET
    @Path("/worker")
    public JobReport worker() {
        return jobs.worker();
    }

    @GET
    @Path("/event-loop")
    public JobReport eventLoop() {
        return jobs.eventLoop();
    }

    @GET
    @Path("/virtual")
    public JobReport virtual() {
        return jobs.virtual();
    }
}
