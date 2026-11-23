package dev.omatheusmesmo.demos.flocioci.oci;

import com.oracle.bmc.secrets.SecretsClient;
import com.oracle.bmc.secrets.model.Base64SecretBundleContentDetails;
import com.oracle.bmc.secrets.requests.GetSecretBundleRequest;
import io.quarkus.arc.Unremovable;
import io.quarkus.credentials.CredentialsProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Gives the datasource its password from the current version of an OCI Vault secret, so the password is never in
 * configuration, in the environment or in the image. Rotating the secret needs no redeploy: new connections pick up
 * the new version.
 */
@ApplicationScoped
@Unremovable
@Named(VaultSecretCredentialsProvider.NAME)
public class VaultSecretCredentialsProvider implements CredentialsProvider {

    public static final String NAME = "oci-vault-secret";

    private final SecretsClient secrets;
    private final DatabaseConfig database;

    public VaultSecretCredentialsProvider(SecretsClient secrets, DatabaseConfig database) {
        this.secrets = secrets;
        this.database = database;
    }

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        Base64SecretBundleContentDetails content = (Base64SecretBundleContentDetails) secrets
                .getSecretBundle(GetSecretBundleRequest.builder()
                        .secretId(database.passwordSecretId())
                        .stage(GetSecretBundleRequest.Stage.Current)
                        .build())
                .getSecretBundle()
                .getSecretBundleContent();
        String password = new String(Base64.getDecoder().decode(content.getContent()), StandardCharsets.UTF_8);
        return Map.of(USER_PROPERTY_NAME, database.username(), PASSWORD_PROPERTY_NAME, password);
    }
}
