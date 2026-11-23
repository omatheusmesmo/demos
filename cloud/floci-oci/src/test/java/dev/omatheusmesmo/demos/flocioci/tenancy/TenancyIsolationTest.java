package dev.omatheusmesmo.demos.flocioci.tenancy;

import com.oracle.bmc.Region;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.model.CreateBucketDetails;
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest;
import com.oracle.bmc.objectstorage.requests.GetBucketRequest;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import dev.omatheusmesmo.demos.flocioci.oci.ThrowawayKey;
import dev.omatheusmesmo.demos.flocioci.support.FlociOciContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * One floci-oci, two tenancies. The emulator never verifies a signature, but it does read the tenancy from the
 * signature's {@code keyId} and keeps each tenancy's resources apart, so two test suites (or two teams) can share one
 * container without seeing each other's buckets. Tenancy B even asks for tenancy A's compartment by its OCID and gets
 * the answer real OCI gives for a compartment you cannot see: {@code 404 NotAuthorizedOrNotFound}.
 */
class TenancyIsolationTest {

    private static final String TENANCY_A = FlociOciContainer.DEFAULT_TENANCY;
    private static final String TENANCY_B =
            "ocid1.tenancy.oc1..tenancyb00000000000000000000000000000000000000000000000000000";

    private static final FlociOciContainer FLOCI = new FlociOciContainer();
    private static ObjectStorageClient tenancyA;
    private static ObjectStorageClient tenancyB;

    @BeforeAll
    static void start() {
        FLOCI.start();
        tenancyA = clientFor(TENANCY_A);
        tenancyB = clientFor(TENANCY_B);
    }

    @AfterAll
    static void stop() {
        tenancyA.close();
        tenancyB.close();
        FLOCI.stop();
    }

    @Test
    void aBucketOfOneTenancyIsInvisibleToTheOther() {
        createBucket(tenancyA, TENANCY_A, "invoices");

        assertThat(bucketNames(tenancyA, TENANCY_A)).contains("invoices");
        assertThatThrownBy(() -> bucketNames(tenancyB, TENANCY_A))
                .isInstanceOfSatisfying(BmcException.class, e -> assertNotFound(e, "NotAuthorizedOrNotFound"));
        assertThatThrownBy(() -> tenancyB.getBucket(GetBucketRequest.builder()
                .namespaceName(namespace(tenancyB)).bucketName("invoices").build()))
                .isInstanceOfSatisfying(BmcException.class, e -> assertNotFound(e, "BucketNotFound"));
    }

    /**
     * Without isolation the second create would fail with {@code 409 BucketAlreadyExists}.
     */
    @Test
    void bothTenanciesCanOwnABucketWithTheSameName() {
        createBucket(tenancyA, TENANCY_A, "reports");
        createBucket(tenancyB, TENANCY_B, "reports");

        assertThat(bucketNames(tenancyA, TENANCY_A)).contains("reports");
        assertThat(bucketNames(tenancyB, TENANCY_B)).contains("reports");
    }

    private static void assertNotFound(BmcException e, String code) {
        assertThat(e.getStatusCode()).isEqualTo(404);
        assertThat(e.getServiceCode()).isEqualTo(code);
    }

    private static ObjectStorageClient clientFor(String tenancyId) {
        ObjectStorageClient client = ObjectStorageClient.builder()
                .region(Region.US_ASHBURN_1)
                .build(ThrowawayKey.signer(tenancyId, "ocid1.user.oc1..isolationtest", Region.US_ASHBURN_1));
        client.setEndpoint(FLOCI.endpoint());
        return client;
    }

    private static String namespace(ObjectStorageClient client) {
        return client.getNamespace(GetNamespaceRequest.builder().build()).getValue();
    }

    private static void createBucket(ObjectStorageClient client, String tenancyId, String name) {
        client.createBucket(CreateBucketRequest.builder()
                .namespaceName(namespace(client))
                .createBucketDetails(CreateBucketDetails.builder().name(name).compartmentId(tenancyId).build())
                .build());
    }

    private static List<String> bucketNames(ObjectStorageClient client, String compartmentId) {
        return client.listBuckets(ListBucketsRequest.builder()
                        .namespaceName(namespace(client))
                        .compartmentId(compartmentId)
                        .build())
                .getItems().stream().map(BucketSummary::getName).toList();
    }
}
