package dev.omatheusmesmo.demos.flocioci.order;

import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Keeps one JSON receipt per order in an Object Storage bucket.
 */
@ApplicationScoped
public class ReceiptStore {

    private final ObjectStorageClient objectStorage;
    private final OrdersConfig.Receipts receipts;

    public ReceiptStore(ObjectStorageClient objectStorage, OrdersConfig config) {
        this.objectStorage = objectStorage;
        this.receipts = config.receipts();
    }

    public String objectName(long orderId) {
        return "receipts/" + orderId + ".json";
    }

    public void put(String objectName, String json) {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        objectStorage.putObject(PutObjectRequest.builder()
                .namespaceName(receipts.namespace())
                .bucketName(receipts.bucket())
                .objectName(objectName)
                .contentType("application/json")
                .contentLength((long) body.length)
                .putObjectBody(new ByteArrayInputStream(body))
                .build());
    }

    public String get(String objectName) {
        GetObjectResponse response = objectStorage.getObject(GetObjectRequest.builder()
                .namespaceName(receipts.namespace())
                .bucketName(receipts.bucket())
                .objectName(objectName)
                .build());
        try (InputStream body = response.getInputStream()) {
            return new String(body.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read receipt " + objectName, e);
        }
    }
}
