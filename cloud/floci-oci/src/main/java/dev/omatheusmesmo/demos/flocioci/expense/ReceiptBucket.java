package dev.omatheusmesmo.demos.flocioci.expense;

import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails;
import com.oracle.bmc.objectstorage.model.PreauthenticatedRequest;
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest;
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.HeadObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * The receipts bucket. Employees upload to {@code incoming/<id>} through a pre-authenticated request, so the file never
 * passes through the application; the worker moves it, encrypted, to {@code receipts/<id>}.
 */
@ApplicationScoped
public class ReceiptBucket {

    private final ObjectStorageClient objectStorage;
    private final ExpensesConfig.Storage storage;

    public ReceiptBucket(ObjectStorageClient objectStorage, ExpensesConfig config) {
        this.objectStorage = objectStorage;
        this.storage = config.storage();
    }

    public static String incoming(UUID expenseId) {
        return "incoming/" + expenseId;
    }

    public static String stored(UUID expenseId) {
        return "receipts/" + expenseId;
    }

    /**
     * A URL that accepts one write of {@code incoming/<id>} until it expires. Real OCI returns it complete as
     * {@code fullPath}; floci-oci 0.7.0 returns a relative {@code fullPath} (floci-io/floci-oci#144), so a relative
     * one is resolved against the Object Storage endpoint, as the relative {@code accessUri} would be.
     */
    public String uploadUrl(UUID expenseId, Instant expiresAt) {
        PreauthenticatedRequest par = objectStorage.createPreauthenticatedRequest(
                CreatePreauthenticatedRequestRequest.builder()
                        .namespaceName(storage.namespace())
                        .bucketName(storage.bucket())
                        .createPreauthenticatedRequestDetails(CreatePreauthenticatedRequestDetails.builder()
                                .name("receipt-upload-" + expenseId)
                                .objectName(incoming(expenseId))
                                .accessType(CreatePreauthenticatedRequestDetails.AccessType.ObjectWrite)
                                .timeExpires(Date.from(expiresAt))
                                .build())
                        .build())
                .getPreauthenticatedRequest();
        String fullPath = par.getFullPath();
        if (fullPath != null && URI.create(fullPath).isAbsolute()) {
            return fullPath;
        }
        return objectStorage.getEndpoint() + par.getAccessUri();
    }

    public OptionalLong size(String objectName) {
        try {
            return OptionalLong.of(objectStorage.headObject(HeadObjectRequest.builder()
                    .namespaceName(storage.namespace())
                    .bucketName(storage.bucket())
                    .objectName(objectName)
                    .build()).getContentLength());
        } catch (BmcException e) {
            if (e.getStatusCode() == 404) {
                return OptionalLong.empty();
            }
            throw e;
        }
    }

    public byte[] read(String objectName) {
        try (InputStream body = objectStorage.getObject(GetObjectRequest.builder()
                .namespaceName(storage.namespace())
                .bucketName(storage.bucket())
                .objectName(objectName)
                .build()).getInputStream()) {
            return body.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + objectName, e);
        }
    }

    public void write(String objectName, byte[] body) {
        objectStorage.putObject(PutObjectRequest.builder()
                .namespaceName(storage.namespace())
                .bucketName(storage.bucket())
                .objectName(objectName)
                .contentType("application/octet-stream")
                .contentLength((long) body.length)
                .putObjectBody(new ByteArrayInputStream(body))
                .build());
    }

    public void delete(String objectName) {
        objectStorage.deleteObject(DeleteObjectRequest.builder()
                .namespaceName(storage.namespace())
                .bucketName(storage.bucket())
                .objectName(objectName)
                .build());
    }
}
