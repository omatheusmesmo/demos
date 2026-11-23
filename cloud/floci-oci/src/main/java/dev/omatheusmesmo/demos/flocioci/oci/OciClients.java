package dev.omatheusmesmo.demos.flocioci.oci;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.keymanagement.KmsCryptoClient;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.secrets.SecretsClient;
import dev.omatheusmesmo.demos.flocioci.expense.ExpensesConfig;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Produces the OCI SDK clients. There is no Quarkus extension for the OCI SDK, so this is the whole integration:
 * one signer, one client per service, and the endpoint each client must talk to.
 */
@Singleton
public class OciClients {

    @Produces
    @Singleton
    AuthenticationDetailsProvider signer(OciConfig config) {
        OciConfig.Auth auth = config.auth();
        return switch (auth.mode()) {
            case THROWAWAY_KEY -> ThrowawayKey.signer(
                    auth.tenancyId().orElseThrow(() -> missing("oci.auth.tenancy-id")),
                    auth.userId().orElseThrow(() -> missing("oci.auth.user-id")),
                    Region.fromRegionId(config.region()));
            case CONFIG_FILE -> fromConfigFile(auth.profile());
        };
    }

    @Produces
    @Singleton
    ObjectStorageClient objectStorage(AuthenticationDetailsProvider signer, OciConfig config) {
        ObjectStorageClient client = ObjectStorageClient.builder()
                .region(Region.fromRegionId(config.region()))
                .build(signer);
        config.endpoint().ifPresent(client::setEndpoint);
        return client;
    }

    @Produces
    @Singleton
    QueueClient queue(AuthenticationDetailsProvider signer, ExpensesConfig expenses) {
        return QueueClient.builder()
                .endpoint(expenses.jobs().messagesEndpoint())
                .build(signer);
    }

    @Produces
    @Singleton
    KmsCryptoClient kmsCrypto(AuthenticationDetailsProvider signer, ExpensesConfig expenses) {
        return KmsCryptoClient.builder()
                .endpoint(expenses.keys().cryptoEndpoint())
                .build(signer);
    }

    @Produces
    @Singleton
    SecretsClient secrets(AuthenticationDetailsProvider signer, OciConfig config) {
        SecretsClient client = SecretsClient.builder()
                .region(Region.fromRegionId(config.region()))
                .build(signer);
        config.endpoint().ifPresent(client::setEndpoint);
        return client;
    }

    void close(@Disposes ObjectStorageClient client) {
        client.close();
    }

    void close(@Disposes QueueClient client) {
        client.close();
    }

    void close(@Disposes KmsCryptoClient client) {
        client.close();
    }

    void close(@Disposes SecretsClient client) {
        client.close();
    }

    private static AuthenticationDetailsProvider fromConfigFile(String profile) {
        try {
            return new ConfigFileAuthenticationDetailsProvider(ConfigFileReader.parseDefault(profile));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read profile " + profile + " of ~/.oci/config", e);
        }
    }

    private static IllegalStateException missing(String property) {
        return new IllegalStateException(property + " is required when oci.auth.mode=throwaway-key");
    }
}
