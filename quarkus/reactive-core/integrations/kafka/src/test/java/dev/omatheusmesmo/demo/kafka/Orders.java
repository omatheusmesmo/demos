package dev.omatheusmesmo.demo.kafka;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;

import io.quarkus.test.common.DevServicesContext;
import io.restassured.response.ValidatableResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Sends orders straight to Kafka, one at a time and in order, and waits for a consumer to report them. Each
 * payload is {@code sequence:processingMillis}. The broker address comes from Dev Services, so the same code
 * works in {@code @QuarkusTest} and in {@code @QuarkusIntegrationTest}.
 */
final class Orders {

    private Orders() {
    }

    static void send(DevServicesContext devServices, String topic, String... payloads) {
        Map<String, Object> config = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, devServices.devServicesProperties().get("kafka.bootstrap.servers"));
        try (var producer = new KafkaProducer<>(config, new StringSerializer(), new StringSerializer())) {
            for (String payload : payloads) {
                producer.send(new ProducerRecord<>(topic, payload)).get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while sending orders", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Kafka rejected an order", e.getCause());
        }
    }

    static ValidatableResponse awaitProcessed(String consumer, int count) {
        await().atMost(Duration.ofSeconds(30)).until(() -> processed(consumer) >= count);
        return get("/consumers/" + consumer).then().statusCode(200);
    }

    private static int processed(String consumer) {
        return get("/consumers/" + consumer).then().statusCode(200).extract().jsonPath().getList("processed").size();
    }
}
