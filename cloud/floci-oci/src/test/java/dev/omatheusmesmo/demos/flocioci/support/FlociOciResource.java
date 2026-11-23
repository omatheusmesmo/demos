package dev.omatheusmesmo.demos.flocioci.support;

import com.oracle.bmc.Region;
import dev.omatheusmesmo.demos.flocioci.oci.ThrowawayKey;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

/**
 * Starts floci-oci, provisions the expenses resources in it and hands their identifiers to the application, the way
 * {@code make provision} does from the Terraform outputs in dev mode. Tests read the values back as configuration
 * ({@code ExpensesConfig}, {@code oci.endpoint}), never through static state: test resources and tests may not share a
 * class loader.
 */
public class FlociOciResource implements QuarkusTestResourceLifecycleManager {

    private static final String PROVISIONER =
            "ocid1.user.oc1..provisioner000000000000000000000000000000000000000000000000";

    private FlociOciContainer container;

    @Override
    public Map<String, String> start() {
        container = new FlociOciContainer();
        container.start();
        OciProvisioner.Resources resources = OciProvisioner.provision(container.endpoint(),
                ThrowawayKey.signer(FlociOciContainer.DEFAULT_TENANCY, PROVISIONER, Region.US_ASHBURN_1),
                FlociOciContainer.DEFAULT_TENANCY);
        return Map.of(
                "oci.endpoint", container.endpoint(),
                "expenses.storage.namespace", resources.namespace(),
                "expenses.storage.bucket", resources.bucket(),
                "expenses.jobs.queue-id", resources.queueId(),
                "expenses.jobs.messages-endpoint", resources.messagesEndpoint(),
                "expenses.keys.key-id", resources.keyId(),
                "expenses.keys.crypto-endpoint", resources.cryptoEndpoint(),
                "expenses.database.username", OciProvisioner.DATABASE_USER,
                "expenses.database.password-secret-id", resources.passwordSecretId(),
                "quarkus.datasource.jdbc.url", resources.jdbcUrl());
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
