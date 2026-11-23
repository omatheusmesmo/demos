package dev.omatheusmesmo.demos.flocioci.oci;

import io.smallrye.config.ConfigMapping;

/**
 * Who the application connects to PostgreSQL as. The password is not configuration: it lives in an OCI Vault secret,
 * read when the connection pool needs it.
 */
@ConfigMapping(prefix = "expenses.database")
public interface DatabaseConfig {

    String username();

    String passwordSecretId();
}
