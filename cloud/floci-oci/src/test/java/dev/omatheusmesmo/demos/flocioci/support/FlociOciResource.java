package dev.omatheusmesmo.demos.flocioci.support;

import com.oracle.bmc.Region;
import dev.omatheusmesmo.demos.flocioci.oci.ThrowawayKey;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

/**
 * Starts floci-oci, provisions the orders resources in it and hands their identifiers to the application, the way
 * {@code make provision} does from the Terraform outputs in dev mode. Tests read the values back as configuration
 * ({@code OrdersConfig}, {@code oci.endpoint}), never through static state: test resources and tests may not share a
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
                "orders.receipts.namespace", resources.namespace(),
                "orders.receipts.bucket", resources.bucket(),
                "orders.events.queue-id", resources.queueId(),
                "orders.events.messages-endpoint", resources.messagesEndpoint(),
                "orders.cards.key-id", resources.keyId(),
                "orders.cards.crypto-endpoint", resources.cryptoEndpoint());
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
