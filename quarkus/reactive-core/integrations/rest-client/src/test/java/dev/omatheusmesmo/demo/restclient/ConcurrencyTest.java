package dev.omatheusmesmo.demo.restclient;

import static io.restassured.RestAssured.delete;
import static io.restassured.RestAssured.get;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import dev.omatheusmesmo.demo.testsupport.Concurrently;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Twelve concurrent requests against a worker pool capped at four threads. The worker variant queues behind the
 * pool; the reactive and virtual thread variants do not depend on it.
 */
@QuarkusTest
@SlowUpstream
@TestProfile(ConcurrencyTest.FourWorkers.class)
class ConcurrencyTest {

    private static final int REQUESTS = 12;
    private static final int WORKERS = 4;

    @Test
    void theWorkerVariantNeverRunsMoreRequestsThanTheWorkerPool() {
        assertThat(peakOf("/quotes/worker"), lessThanOrEqualTo(WORKERS));
    }

    @Test
    void theReactiveVariantServesEveryRequestAtOnce() {
        assertThat(peakOf("/quotes/reactive"), is(REQUESTS));
    }

    @Test
    void theVirtualVariantServesEveryRequestAtOnce() {
        assertThat(peakOf("/quotes/virtual"), is(REQUESTS));
    }

    private static int peakOf(String path) {
        delete("/quotes/peak").then().statusCode(204);
        Concurrently.get(REQUESTS, path).forEach(response -> response.then().statusCode(200));
        return Integer.parseInt(get("/quotes/peak").then().statusCode(200).extract().asString());
    }

    public static class FourWorkers implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.thread-pool.max-threads", String.valueOf(WORKERS));
        }
    }
}
