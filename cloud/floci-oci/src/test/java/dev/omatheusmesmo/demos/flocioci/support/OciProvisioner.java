package dev.omatheusmesmo.demos.flocioci.support;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.keymanagement.KmsManagementClient;
import com.oracle.bmc.keymanagement.KmsVaultClient;
import com.oracle.bmc.keymanagement.model.CreateKeyDetails;
import com.oracle.bmc.keymanagement.model.CreateVaultDetails;
import com.oracle.bmc.keymanagement.model.KeyShape;
import com.oracle.bmc.keymanagement.model.Vault;
import com.oracle.bmc.keymanagement.requests.CreateKeyRequest;
import com.oracle.bmc.keymanagement.requests.CreateVaultRequest;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.CreateBucketDetails;
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.queue.QueueAdminClient;
import com.oracle.bmc.queue.model.CreateQueueDetails;
import com.oracle.bmc.queue.model.Queue;
import com.oracle.bmc.queue.requests.CreateQueueRequest;
import com.oracle.bmc.queue.requests.GetQueueRequest;
import com.oracle.bmc.queue.requests.GetWorkRequestRequest;

/**
 * Creates the resources the orders service needs, through the same SDK calls Terraform makes. It follows the OCI
 * contract rather than shortcuts: the queue OCID comes from the work request, and the data plane endpoints come from
 * the vault and the queue themselves.
 */
public final class OciProvisioner {

    public record Resources(String namespace, String bucket, String queueId, String messagesEndpoint, String keyId,
                            String cryptoEndpoint) {
    }

    private OciProvisioner() {
    }

    public static Resources provision(String endpoint, AuthenticationDetailsProvider signer, String compartmentId) {
        String namespace;
        String bucket = "order-receipts";
        try (ObjectStorageClient objectStorage = ObjectStorageClient.builder().region(Region.US_ASHBURN_1).build(signer)) {
            objectStorage.setEndpoint(endpoint);
            namespace = objectStorage.getNamespace(GetNamespaceRequest.builder().build()).getValue();
            objectStorage.createBucket(CreateBucketRequest.builder()
                    .namespaceName(namespace)
                    .createBucketDetails(CreateBucketDetails.builder().name(bucket).compartmentId(compartmentId).build())
                    .build());
        }

        Vault vault;
        try (KmsVaultClient vaults = KmsVaultClient.builder().region(Region.US_ASHBURN_1).build(signer)) {
            vaults.setEndpoint(endpoint);
            vault = vaults.createVault(CreateVaultRequest.builder()
                    .createVaultDetails(CreateVaultDetails.builder()
                            .compartmentId(compartmentId)
                            .displayName("orders-vault")
                            .vaultType(CreateVaultDetails.VaultType.Default)
                            .build())
                    .build()).getVault();
        }

        String keyId;
        try (KmsManagementClient keys = KmsManagementClient.builder()
                .endpoint(vault.getManagementEndpoint()).build(signer)) {
            keyId = keys.createKey(CreateKeyRequest.builder()
                    .createKeyDetails(CreateKeyDetails.builder()
                            .compartmentId(compartmentId)
                            .displayName("card-numbers")
                            .keyShape(KeyShape.builder().algorithm(KeyShape.Algorithm.Aes).length(32).build())
                            .build())
                    .build()).getKey().getId();
        }

        Queue queue;
        try (QueueAdminClient queues = QueueAdminClient.builder().region(Region.US_ASHBURN_1).build(signer)) {
            queues.setEndpoint(endpoint);
            String workRequestId = queues.createQueue(CreateQueueRequest.builder()
                    .createQueueDetails(CreateQueueDetails.builder()
                            .compartmentId(compartmentId)
                            .displayName("order-events")
                            .build())
                    .build()).getOpcWorkRequestId();
            String queueId = queues.getWorkRequest(GetWorkRequestRequest.builder().workRequestId(workRequestId).build())
                    .getWorkRequest().getResources().getFirst().getIdentifier();
            queue = queues.getQueue(GetQueueRequest.builder().queueId(queueId).build()).getQueue();
        }

        return new Resources(namespace, bucket, queue.getId(), queue.getMessagesEndpoint(), keyId,
                vault.getCryptoEndpoint());
    }
}
