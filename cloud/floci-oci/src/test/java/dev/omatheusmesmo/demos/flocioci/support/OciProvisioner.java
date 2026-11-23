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
import com.oracle.bmc.psql.PostgresqlClient;
import com.oracle.bmc.psql.model.ConnectionDetails;
import com.oracle.bmc.psql.model.CreateDbSystemDetails;
import com.oracle.bmc.psql.model.Credentials;
import com.oracle.bmc.psql.model.DbSystem;
import com.oracle.bmc.psql.model.NetworkDetails;
import com.oracle.bmc.psql.model.OciOptimizedStorageDetails;
import com.oracle.bmc.psql.model.VaultSecretPasswordDetails;
import com.oracle.bmc.psql.requests.CreateDbSystemRequest;
import com.oracle.bmc.psql.requests.GetConnectionDetailsRequest;
import com.oracle.bmc.psql.requests.GetDbSystemRequest;
import com.oracle.bmc.queue.QueueAdminClient;
import com.oracle.bmc.queue.model.CreateQueueDetails;
import com.oracle.bmc.queue.model.Queue;
import com.oracle.bmc.queue.requests.CreateQueueRequest;
import com.oracle.bmc.queue.requests.GetQueueRequest;
import com.oracle.bmc.queue.requests.GetWorkRequestRequest;
import com.oracle.bmc.vault.VaultsClient;
import com.oracle.bmc.vault.model.Base64SecretContentDetails;
import com.oracle.bmc.vault.model.CreateSecretDetails;
import com.oracle.bmc.vault.requests.CreateSecretRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Creates the resources of the expenses service with the same API calls the Terraform module makes. It follows the
 * OCI contract rather than shortcuts: the queue OCID comes from the work request, the data plane endpoints come from
 * the vault and the queue, and the database endpoint and CA certificate come from the DB system's connection details.
 */
public final class OciProvisioner {

    public static final String DATABASE_USER = "expenses";
    private static final Duration DATABASE_READY_TIMEOUT = Duration.ofMinutes(3);
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Resources(String namespace, String bucket, String queueId, String messagesEndpoint, String keyId,
                            String cryptoEndpoint, String passwordSecretId, String jdbcUrl) {
    }

    private final String endpoint;
    private final AuthenticationDetailsProvider signer;
    private final String compartmentId;

    private OciProvisioner(String endpoint, AuthenticationDetailsProvider signer, String compartmentId) {
        this.endpoint = endpoint;
        this.signer = signer;
        this.compartmentId = compartmentId;
    }

    public static Resources provision(String endpoint, AuthenticationDetailsProvider signer, String compartmentId) {
        return new OciProvisioner(endpoint, signer, compartmentId).provision();
    }

    private Resources provision() {
        String bucket = "expense-receipts";
        String namespace = createBucket(bucket);
        Vault vault = createVault();
        String keyId = createKey(vault);
        Queue queue = createQueue();
        String passwordSecretId = createSecret(vault, keyId, randomPassword());
        String jdbcUrl = createDatabase(passwordSecretId);
        return new Resources(namespace, bucket, queue.getId(), queue.getMessagesEndpoint(), keyId,
                vault.getCryptoEndpoint(), passwordSecretId, jdbcUrl);
    }

    private String createBucket(String bucket) {
        try (ObjectStorageClient objectStorage = ObjectStorageClient.builder().region(Region.US_ASHBURN_1).build(signer)) {
            objectStorage.setEndpoint(endpoint);
            String namespace = objectStorage.getNamespace(GetNamespaceRequest.builder().build()).getValue();
            objectStorage.createBucket(CreateBucketRequest.builder()
                    .namespaceName(namespace)
                    .createBucketDetails(CreateBucketDetails.builder().name(bucket).compartmentId(compartmentId).build())
                    .build());
            return namespace;
        }
    }

    private Vault createVault() {
        try (KmsVaultClient vaults = KmsVaultClient.builder().region(Region.US_ASHBURN_1).build(signer)) {
            vaults.setEndpoint(endpoint);
            return vaults.createVault(CreateVaultRequest.builder()
                    .createVaultDetails(CreateVaultDetails.builder()
                            .compartmentId(compartmentId)
                            .displayName("expenses-vault")
                            .vaultType(CreateVaultDetails.VaultType.Default)
                            .build())
                    .build()).getVault();
        }
    }

    private String createKey(Vault vault) {
        try (KmsManagementClient keys = KmsManagementClient.builder()
                .endpoint(vault.getManagementEndpoint()).build(signer)) {
            return keys.createKey(CreateKeyRequest.builder()
                    .createKeyDetails(CreateKeyDetails.builder()
                            .compartmentId(compartmentId)
                            .displayName("receipts-master-key")
                            .keyShape(KeyShape.builder().algorithm(KeyShape.Algorithm.Aes).length(32).build())
                            .build())
                    .build()).getKey().getId();
        }
    }

    private Queue createQueue() {
        try (QueueAdminClient queues = QueueAdminClient.builder().region(Region.US_ASHBURN_1).build(signer)) {
            queues.setEndpoint(endpoint);
            String workRequestId = queues.createQueue(CreateQueueRequest.builder()
                    .createQueueDetails(CreateQueueDetails.builder()
                            .compartmentId(compartmentId)
                            .displayName("receipt-processing")
                            .deadLetterQueueDeliveryCount(3)
                            .build())
                    .build()).getOpcWorkRequestId();
            String queueId = queues.getWorkRequest(GetWorkRequestRequest.builder().workRequestId(workRequestId).build())
                    .getWorkRequest().getResources().getFirst().getIdentifier();
            return queues.getQueue(GetQueueRequest.builder().queueId(queueId).build()).getQueue();
        }
    }

    private String createSecret(Vault vault, String keyId, String password) {
        try (VaultsClient secrets = VaultsClient.builder().region(Region.US_ASHBURN_1).build(signer)) {
            secrets.setEndpoint(endpoint);
            return secrets.createSecret(CreateSecretRequest.builder()
                    .createSecretDetails(CreateSecretDetails.builder()
                            .compartmentId(compartmentId)
                            .vaultId(vault.getId())
                            .keyId(keyId)
                            .secretName("expenses-db-password")
                            .secretContent(Base64SecretContentDetails.builder()
                                    .content(Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8)))
                                    .build())
                            .build())
                    .build()).getSecret().getId();
        }
    }

    /**
     * Creates the DB system with its admin password taken from the secret, waits until it is {@code ACTIVE} and
     * returns a JDBC URL that verifies the server against the DB system's own CA certificate.
     */
    private String createDatabase(String passwordSecretId) {
        try (PostgresqlClient postgresql = PostgresqlClient.builder().region(Region.US_ASHBURN_1).build(signer)) {
            postgresql.setEndpoint(endpoint);
            String dbSystemId = postgresql.createDbSystem(CreateDbSystemRequest.builder()
                    .createDbSystemDetails(CreateDbSystemDetails.builder()
                            .compartmentId(compartmentId)
                            .displayName("expenses-db")
                            .dbVersion("16")
                            .shape("PostgreSQL.VM.Standard.E4.Flex")
                            .instanceOcpuCount(2)
                            .instanceMemorySizeInGBs(32)
                            .networkDetails(NetworkDetails.builder().subnetId("ocid1.subnet.oc1.iad.expenses").build())
                            .storageDetails(OciOptimizedStorageDetails.builder().isRegionallyDurable(true).build())
                            .credentials(Credentials.builder()
                                    .username(DATABASE_USER)
                                    .passwordDetails(VaultSecretPasswordDetails.builder()
                                            .secretId(passwordSecretId)
                                            .secretVersion("1")
                                            .build())
                                    .build())
                            .build())
                    .build()).getDbSystem().getId();
            awaitActive(postgresql, dbSystemId);

            ConnectionDetails connection = postgresql.getConnectionDetails(GetConnectionDetailsRequest.builder()
                    .dbSystemId(dbSystemId).build()).getConnectionDetails();
            Path caCertificate = writeTempFile("expenses-db-ca", connection.getCaCertificate());
            return "jdbc:postgresql://%s:%d/postgres?sslmode=verify-full&sslrootcert=%s".formatted(
                    connection.getPrimaryDbEndpoint().getFqdn(), connection.getPrimaryDbEndpoint().getPort(),
                    caCertificate);
        }
    }

    private static void awaitActive(PostgresqlClient postgresql, String dbSystemId) {
        Instant deadline = Instant.now().plus(DATABASE_READY_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            DbSystem.LifecycleState state = postgresql.getDbSystem(GetDbSystemRequest.builder()
                    .dbSystemId(dbSystemId).build()).getDbSystem().getLifecycleState();
            if (state == DbSystem.LifecycleState.Active) {
                return;
            }
            if (state == DbSystem.LifecycleState.Failed) {
                throw new IllegalStateException("DB system " + dbSystemId + " failed to start");
            }
            sleep(Duration.ofSeconds(1));
        }
        throw new IllegalStateException("DB system " + dbSystemId + " not ACTIVE after " + DATABASE_READY_TIMEOUT);
    }

    private static String randomPassword() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static Path writeTempFile(String prefix, String content) {
        try {
            Path file = Files.createTempFile(prefix, ".pem");
            file.toFile().deleteOnExit();
            return Files.writeString(file, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
