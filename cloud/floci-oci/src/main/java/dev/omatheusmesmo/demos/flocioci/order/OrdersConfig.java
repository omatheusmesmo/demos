package dev.omatheusmesmo.demos.flocioci.order;

import io.smallrye.config.ConfigMapping;

/**
 * The OCI resources the orders service uses. The application never creates them: Terraform does in dev, the test
 * resource does in tests. The two data plane endpoints come from the resources themselves on real OCI (the vault's
 * crypto endpoint, the queue's messages endpoint), which is why they are configuration and not derived from the region.
 */
@ConfigMapping(prefix = "orders")
public interface OrdersConfig {

    Receipts receipts();

    Events events();

    Cards cards();

    interface Receipts {

        String namespace();

        String bucket();
    }

    interface Events {

        String queueId();

        String messagesEndpoint();
    }

    interface Cards {

        String keyId();

        String cryptoEndpoint();
    }
}
