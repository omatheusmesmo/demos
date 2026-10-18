package dev.omatheusmesmo.demo.postgres;

import static io.restassured.RestAssured.delete;
import static io.restassured.RestAssured.get;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import dev.omatheusmesmo.demo.testsupport.Concurrently;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Virtual threads remove the limit of the worker pool, not the limit of the connection pool. Twelve requests run
 * at the same time, but only four of them hold a connection; the others wait for one.
 */
@QuarkusTest
@TestProfile(PoolLimitTest.FourConnections.class)
class PoolLimitTest {

    private static final int REQUESTS = 12;
    private static final int CONNECTIONS = 4;

    @Test
    void theConnectionPoolIsTheLimitForVirtualThreads() {
        delete("/books/peak").then().statusCode(204);
        delete("/books/pool").then().statusCode(204);

        Concurrently.get(REQUESTS, "/books/virtual").forEach(response -> response.then().statusCode(200));

        assertThat(number("/books/peak"), is(REQUESTS));
        assertThat(number("/books/pool"), is(CONNECTIONS));
    }

    private static int number(String path) {
        return Integer.parseInt(get(path).then().statusCode(200).extract().asString());
    }

    public static class FourConnections implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.datasource.jdbc.max-size", String.valueOf(CONNECTIONS));
        }
    }
}
