package dev.omatheusmesmo.demo.restclient;

import io.quarkus.arc.profile.IfBuildProfile;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.time.Duration;

/**
 * A stand-in for the slow upstream, available only in dev mode so the demo runs without any setup. Tests use
 * WireMock and the benchmark runs a separate WireMock container instead.
 */
@IfBuildProfile("dev")
@Path("/upstream/quote")
public class UpstreamSimulator {

    @GET
    public Uni<Quote> quote() {
        return Uni.createFrom().item(new Quote("stay reactive")).onItem().delayIt().by(Duration.ofMillis(300));
    }
}
