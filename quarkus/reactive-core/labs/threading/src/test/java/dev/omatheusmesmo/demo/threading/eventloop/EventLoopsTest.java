package dev.omatheusmesmo.demo.threading.eventloop;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;

import dev.omatheusmesmo.demo.testsupport.Concurrently;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(EventLoopsTest.TwoEventLoops.class)
class EventLoopsTest {

    @Test
    void everyNonBlockingRequestIsServedByOneOfTheConfiguredEventLoops() {
        Set<String> threads = Concurrently.get(24, "/event-loops/current").stream()
                .map(response -> response.jsonPath().getString("thread.name"))
                .collect(Collectors.toSet());

        assertThat(threads, hasSize(lessThanOrEqualTo(2)));
        assertThat(threads, everyItem(startsWith("vert.x-eventloop-thread-")));
    }

    public static class TwoEventLoops implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.vertx.event-loops-pool-size", "2");
        }
    }
}
