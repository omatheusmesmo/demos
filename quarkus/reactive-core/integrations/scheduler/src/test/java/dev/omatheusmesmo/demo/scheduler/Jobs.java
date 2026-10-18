package dev.omatheusmesmo.demo.scheduler;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;

import io.restassured.response.ValidatableResponse;
import java.time.Duration;

final class Jobs {

    private Jobs() {
    }

    static ValidatableResponse awaitExecutions(String job, int count) {
        await().atMost(Duration.ofSeconds(15)).until(() -> executions(job) >= count);
        return get("/jobs/" + job).then().statusCode(200);
    }

    static int executions(String job) {
        return get("/jobs/" + job).then().statusCode(200).extract().jsonPath().getInt("executions");
    }
}
