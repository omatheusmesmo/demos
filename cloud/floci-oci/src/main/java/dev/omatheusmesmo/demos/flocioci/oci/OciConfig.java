package dev.omatheusmesmo.demos.flocioci.oci;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

/**
 * How the application reaches OCI. On real OCI only {@code region} and the auth profile matter; against floci-oci
 * {@code endpoint} sends every control plane call to the emulator and the auth mode signs with a throwaway key.
 */
@ConfigMapping(prefix = "oci")
public interface OciConfig {

    String region();

    /**
     * Overrides the regional endpoint of every client built from the region. Empty on real OCI.
     */
    Optional<String> endpoint();

    Auth auth();

    interface Auth {

        @WithDefault("config-file")
        Mode mode();

        /**
         * Profile of {@code ~/.oci/config}, used by {@link Mode#CONFIG_FILE}.
         */
        @WithDefault("DEFAULT")
        String profile();

        /**
         * Tenancy written in the signature of {@link Mode#THROWAWAY_KEY}. floci-oci partitions its storage by it.
         */
        Optional<String> tenancyId();

        Optional<String> userId();

        enum Mode {
            /**
             * Reads {@code ~/.oci/config}, as the OCI CLI does.
             */
            CONFIG_FILE,
            /**
             * Signs with an RSA key generated at startup. Only valid against an emulator that never verifies signatures.
             */
            THROWAWAY_KEY
        }
    }
}
