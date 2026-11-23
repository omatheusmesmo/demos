package dev.omatheusmesmo.demos.flocioci.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.GetMessage;
import com.oracle.bmc.queue.requests.GetMessagesRequest;
import dev.omatheusmesmo.demos.flocioci.support.FlociOciResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

/**
 * Places orders through the REST API, then checks each OCI service directly, not through the application: the row in
 * Oracle Database, the object in the bucket, the message in the queue.
 */
@QuarkusTest
@WithTestResource(FlociOciResource.class)
class OrderResourceTest {

    private static final String CARD = "4111111111114242";

    @Inject
    OrdersConfig config;

    @Inject
    ObjectStorageClient objectStorage;

    @Inject
    QueueClient queue;

    @Inject
    CardVault cardVault;

    @Inject
    ObjectMapper json;

    @Test
    void returnsThePlacedOrderWithoutTheCardNumber() {
        ExtractableResponse<Response> created = given().contentType(ContentType.JSON)
                .body(order("ana", "129.90"))
                .when().post("/orders")
                .then().statusCode(201)
                .body("customer", equalTo("ana"))
                .body("cardLast4", equalTo("4242"))
                .body("$", not(hasKey("cardNumber")))
                .body("$", not(hasKey("cardCiphertext")))
                .extract();

        assertThat(created.header("Location")).endsWith("/orders/" + created.path("id"));
    }

    @Test
    void storesOnlyTheKmsCiphertextOfTheCardInOracle() {
        long id = place("bruno", "10.00");

        Order row = Order.findById(id);

        assertThat(row.cardCiphertext).isNotBlank().doesNotContain(CARD);
        assertThat(cardVault.decrypt(row.cardCiphertext)).isEqualTo(CARD);
    }

    @Test
    void writesTheReceiptToObjectStorage() throws Exception {
        long id = place("carla", "42.00");

        JsonNode receipt = json.readTree(readObject("receipts/" + id + ".json"));

        assertThat(receipt.path("id").asLong()).isEqualTo(id);
        assertThat(receipt.path("customer").asText()).isEqualTo("carla");
        assertThat(receipt.path("total").decimalValue()).isEqualByComparingTo("42.00");
        assertThat(receipt.path("receiptObject").asText()).isEqualTo("receipts/" + id + ".json");
        given().when().get("/orders/" + id + "/receipt")
                .then().statusCode(200)
                .body("id", equalTo((int) id));
    }

    @Test
    void publishesAnOrderCreatedEventToTheQueue() throws Exception {
        long id = place("davi", "7.50");

        JsonNode event = eventFor(id);

        assertThat(event.path("type").asText()).isEqualTo("order-created");
        assertThat(event.path("order").path("customer").asText()).isEqualTo("davi");
        assertThat(event.path("order").path("receiptObject").asText()).isEqualTo("receipts/" + id + ".json");
    }

    @Test
    void rejectsAnInvalidCardBeforeCallingOci() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"customer": "eva", "total": 5.00, "cardNumber": "1234"}""")
                .when().post("/orders")
                .then().statusCode(400);
    }

    @Test
    void answersNotFoundForAnUnknownOrder() {
        given().when().get("/orders/999999").then().statusCode(404);
        given().when().get("/orders/999999/receipt").then().statusCode(404);
    }

    private long place(String customer, String total) {
        Number id = given().contentType(ContentType.JSON)
                .body(order(customer, total))
                .when().post("/orders")
                .then().statusCode(201)
                .extract().path("id");
        return id.longValue();
    }

    private static String order(String customer, String total) {
        return """
                {"customer": "%s", "total": %s, "cardNumber": "%s"}""".formatted(customer, total, CARD);
    }

    private String readObject(String name) throws Exception {
        try (InputStream body = objectStorage.getObject(GetObjectRequest.builder()
                .namespaceName(config.receipts().namespace())
                .bucketName(config.receipts().bucket())
                .objectName(name)
                .build()).getInputStream()) {
            return new String(body.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Peeks at the queue (visibility 0 leaves every message visible) and picks the event of the given order.
     */
    private JsonNode eventFor(long orderId) throws Exception {
        List<GetMessage> messages = queue.getMessages(GetMessagesRequest.builder()
                .queueId(config.events().queueId())
                .visibilityInSeconds(0)
                .limit(20)
                .build()).getGetMessages().getMessages();
        for (GetMessage message : messages) {
            JsonNode event = json.readTree(message.getContent());
            if (event.path("order").path("id").asLong() == orderId) {
                return event;
            }
        }
        throw new AssertionError("No order-created event for order " + orderId + " among " + messages.size());
    }
}
