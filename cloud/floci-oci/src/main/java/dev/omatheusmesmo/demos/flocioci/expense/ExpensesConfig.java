package dev.omatheusmesmo.demos.flocioci.expense;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.time.Duration;

/**
 * The OCI resources of the expenses service and how it uses them. The application never creates them: Terraform does
 * in dev, the test resource does in tests. The two data plane endpoints come from the resources themselves on real OCI
 * (the queue's messages endpoint, the vault's crypto endpoint), which is why they are configuration.
 */
@ConfigMapping(prefix = "expenses")
public interface ExpensesConfig {

    Storage storage();

    Jobs jobs();

    Keys keys();

    Upload upload();

    interface Storage {

        String namespace();

        String bucket();
    }

    interface Jobs {

        String queueId();

        String messagesEndpoint();

        /**
         * How long a job stays invisible to other consumers while the worker handles it.
         */
        @WithDefault("60")
        int visibilitySeconds();

        @WithDefault("10")
        int longPollSeconds();

        /**
         * How often the worker polls, read by {@code @Scheduled} in {@link ReceiptWorker}.
         */
        @WithDefault("1s")
        String pollEvery();
    }

    interface Keys {

        String keyId();

        String cryptoEndpoint();
    }

    interface Upload {

        @WithDefault("PT15M")
        Duration expiry();

        @WithDefault("10485760")
        long maxBytes();
    }
}
