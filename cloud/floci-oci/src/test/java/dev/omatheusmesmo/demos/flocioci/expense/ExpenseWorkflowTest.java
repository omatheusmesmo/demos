package dev.omatheusmesmo.demos.flocioci.expense;

import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.HeadObjectRequest;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.PutMessagesDetails;
import com.oracle.bmc.queue.model.PutMessagesDetailsEntry;
import com.oracle.bmc.queue.requests.GetStatsRequest;
import com.oracle.bmc.queue.requests.PutMessagesRequest;
import dev.omatheusmesmo.demos.flocioci.support.FlociOciResource;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Drives the whole claim as a client would (open, upload with plain HTTP, submit, download) and checks the OCI side
 * directly: what is in the bucket, what reached the dead-letter queue.
 */
@QuarkusTest
@WithTestResource(FlociOciResource.class)
class ExpenseWorkflowTest {

    private static final byte[] PDF = "%PDF-1.7\n1 0 obj << /Type /Catalog >> endobj\n%%EOF\n"
            .getBytes(StandardCharsets.US_ASCII);
    private static final Duration PROCESSING = Duration.ofSeconds(30);
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Inject
    ExpensesConfig config;

    @Inject
    ObjectStorageClient objectStorage;

    @Inject
    QueueClient queue;

    @Test
    void acceptsAnUploadedPdfAndServesItDecryptedToReviewers() throws Exception {
        Claim claim = open("ana");

        assertThat(upload(claim.uploadUrl(), PDF)).isEqualTo(200);
        submit(claim.id()).statusCode(202).body("status", equalTo("SUBMITTED"));
        awaitStatus(claim.id(), ExpenseStatus.READY_FOR_REVIEW);

        byte[] receipt = given().when().get("/expenses/" + claim.id() + "/receipt")
                .then().statusCode(200)
                .contentType("application/pdf")
                .extract().asByteArray();
        assertThat(receipt).isEqualTo(PDF);
    }

    @Test
    void keepsOnlyTheEncryptedReceiptInTheBucket() throws Exception {
        Claim claim = open("bruno");
        upload(claim.uploadUrl(), PDF);
        submit(claim.id()).statusCode(202);
        awaitStatus(claim.id(), ExpenseStatus.READY_FOR_REVIEW);

        byte[] stored = readObject(ReceiptBucket.stored(claim.id()));

        assertThat(new String(stored, StandardCharsets.ISO_8859_1)).doesNotContain("%PDF");
        assertThat(stored.length).isGreaterThan(PDF.length);
        assertThatThrownBy(() -> headObject(ReceiptBucket.incoming(claim.id())))
                .isInstanceOfSatisfying(BmcException.class, e -> assertThat(e.getStatusCode()).isEqualTo(404));
    }

    @Test
    void rejectsAFileThatIsNotAReceipt() throws Exception {
        Claim claim = open("carla");
        upload(claim.uploadUrl(), "dinner was great".getBytes(StandardCharsets.UTF_8));
        submit(claim.id()).statusCode(202);

        awaitStatus(claim.id(), ExpenseStatus.REJECTED);

        given().when().get("/expenses/" + claim.id())
                .then().body("rejectionReason", containsString("not a PDF, PNG or JPEG"));
        given().when().get("/expenses/" + claim.id() + "/receipt").then().statusCode(409);
    }

    @Test
    void refusesASubmissionWithoutAnUploadedReceipt() {
        Claim claim = open("davi");

        submit(claim.id()).statusCode(409).body("message", containsString("No receipt has been uploaded"));
    }

    @Test
    void refusesToSubmitTheSameClaimTwice() throws Exception {
        Claim claim = open("eva");
        upload(claim.uploadUrl(), PDF);
        submit(claim.id()).statusCode(202);

        submit(claim.id()).statusCode(409);
    }

    @Test
    void theUploadUrlWritesOnlyTheReceiptOfItsOwnClaim() throws Exception {
        Claim claim = open("fabio");
        String otherObject = claim.uploadUrl()
                .replace(ReceiptBucket.incoming(claim.id()), ReceiptBucket.stored(claim.id()));

        assertThat(upload(otherObject, PDF)).isEqualTo(404);
        assertThat(HTTP.send(HttpRequest.newBuilder(URI.create(claim.uploadUrl())).GET().build(),
                HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(404);
    }

    /**
     * The job is enqueued before the submission commits, so the worker can see it first. Holding the claim at
     * {@code AWAITING_RECEIPT} until the worker has taken the job reproduces that window deterministically.
     */
    @Test
    void retriesAJobThatArrivesBeforeTheSubmissionCommits() throws Exception {
        Claim claim = open("ines");
        upload(claim.uploadUrl(), PDF);
        enqueueRaw(claim.id().toString());
        await().atMost(PROCESSING).until(() -> inFlightJobs() > 0);

        QuarkusTransaction.requiringNew().run(() ->
                Expense.update("status = ?1 where id = ?2", ExpenseStatus.SUBMITTED, claim.id()));

        awaitStatus(claim.id(), ExpenseStatus.READY_FOR_REVIEW);
    }

    @Test
    void movesAJobThatKeepsFailingToTheDeadLetterQueue() {
        long deadLettersBefore = deadLetters();
        enqueueRaw("not-an-expense-id");

        await().atMost(PROCESSING).until(() -> deadLetters() > deadLettersBefore);
    }

    @Test
    void listsTheClaimsOfAnEmployeeFromTheDatabase() {
        String employee = "gabi-" + UUID.randomUUID();
        open(employee, "Hotel Aurora");
        open(employee, "Taxi Centro");
        open("someone-else", "Hotel Aurora");

        given().queryParam("employee", employee).when().get("/expenses")
                .then().statusCode(200)
                .body("merchant", containsInAnyOrder("Hotel Aurora", "Taxi Centro"));
    }

    @Test
    void rejectsAnInvalidClaimBeforeTouchingOci() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"employee": "hugo", "merchant": "Cafe", "amount": 0, "currency": "usd", "spentOn": "2999-01-01"}""")
                .when().post("/expenses")
                .then().statusCode(400);
    }

    @Test
    void answersNotFoundForAnUnknownClaim() {
        given().when().get("/expenses/" + UUID.randomUUID()).then().statusCode(404);
    }

    private record Claim(UUID id, String uploadUrl) {
    }

    private Claim open(String employee) {
        return open(employee, "Hotel Aurora");
    }

    private Claim open(String employee, String merchant) {
        var created = given().contentType(ContentType.JSON)
                .body("""
                        {"employee": "%s", "merchant": "%s", "amount": 189.90, "currency": "BRL", "spentOn": "2026-11-20"}"""
                        .formatted(employee, merchant))
                .when().post("/expenses")
                .then().statusCode(201)
                .body("expense.status", equalTo("AWAITING_RECEIPT"))
                .body("upload.method", equalTo("PUT"))
                .extract();
        UUID id = UUID.fromString(created.path("expense.id"));
        assertThat(created.header("Location")).endsWith("/expenses/" + id);
        return new Claim(id, created.path("upload.url"));
    }

    /**
     * Uploads the way a browser or mobile app would: a plain PUT to the pre-authenticated URL, with no OCI signature.
     */
    private static int upload(String url, byte[] body) throws IOException, InterruptedException {
        return HTTP.send(HttpRequest.newBuilder(URI.create(url)).PUT(HttpRequest.BodyPublishers.ofByteArray(body)).build(),
                HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private static io.restassured.response.ValidatableResponse submit(UUID id) {
        return given().when().post("/expenses/" + id + "/submit").then();
    }

    private static void awaitStatus(UUID id, ExpenseStatus status) {
        await().atMost(PROCESSING).until(
                () -> given().when().get("/expenses/" + id).then().extract().path("status"), equalTo(status.name()));
    }

    private byte[] readObject(String name) throws IOException {
        try (InputStream body = objectStorage.getObject(GetObjectRequest.builder()
                .namespaceName(config.storage().namespace())
                .bucketName(config.storage().bucket())
                .objectName(name)
                .build()).getInputStream()) {
            return body.readAllBytes();
        }
    }

    private void headObject(String name) {
        objectStorage.headObject(HeadObjectRequest.builder()
                .namespaceName(config.storage().namespace())
                .bucketName(config.storage().bucket())
                .objectName(name)
                .build());
    }

    private void enqueueRaw(String content) {
        queue.putMessages(PutMessagesRequest.builder()
                .queueId(config.jobs().queueId())
                .putMessagesDetails(PutMessagesDetails.builder()
                        .messages(List.of(PutMessagesDetailsEntry.builder().content(content).build()))
                        .build())
                .build());
    }

    private long inFlightJobs() {
        return queue.getStats(GetStatsRequest.builder().queueId(config.jobs().queueId()).build())
                .getQueueStats().getQueue().getInFlightMessages();
    }

    private long deadLetters() {
        return queue.getStats(GetStatsRequest.builder().queueId(config.jobs().queueId()).build())
                .getQueueStats().getDlq().getVisibleMessages();
    }
}
