package dev.omatheusmesmo.demos.flocioci.order;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.keymanagement.KmsManagementClient;
import com.oracle.bmc.keymanagement.requests.CreateKeyVersionRequest;
import com.oracle.bmc.keymanagement.requests.GetKeyRequest;
import dev.omatheusmesmo.demos.flocioci.support.FlociOciResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * floci-oci does real AES-GCM, not a base64 round trip: rotating the key changes the ciphertext of the same card, and
 * a card encrypted before the rotation still decrypts, because the ciphertext names the key version that produced it.
 */
@QuarkusTest
@WithTestResource(FlociOciResource.class)
class CardVaultRotationTest {

    private static final String CARD = "5500000000000004";

    @Inject
    CardVault cardVault;

    @Inject
    OrdersConfig config;

    @Inject
    AuthenticationDetailsProvider signer;

    @Test
    void decryptsACardEncryptedBeforeTheKeyWasRotated() {
        String beforeRotation = cardVault.encrypt(CARD);

        String newVersion = rotateKey();
        String afterRotation = cardVault.encrypt(CARD);

        assertThat(currentKeyVersion()).isEqualTo(newVersion);
        assertThat(afterRotation).isNotEqualTo(beforeRotation);
        assertThat(cardVault.decrypt(beforeRotation)).isEqualTo(CARD);
        assertThat(cardVault.decrypt(afterRotation)).isEqualTo(CARD);
    }

    @Test
    void encryptsTheSameCardDifferentlyEveryTime() {
        assertThat(cardVault.encrypt(CARD)).isNotEqualTo(cardVault.encrypt(CARD));
    }

    private String rotateKey() {
        try (KmsManagementClient keys = managementClient()) {
            return keys.createKeyVersion(CreateKeyVersionRequest.builder().keyId(config.cards().keyId()).build())
                    .getKeyVersion().getId();
        }
    }

    private String currentKeyVersion() {
        try (KmsManagementClient keys = managementClient()) {
            return keys.getKey(GetKeyRequest.builder().keyId(config.cards().keyId()).build())
                    .getKey().getCurrentKeyVersion();
        }
    }

    /**
     * The management endpoint is the vault's, which floci-oci serves on the same host as the crypto endpoint.
     */
    private KmsManagementClient managementClient() {
        return KmsManagementClient.builder().endpoint(config.cards().cryptoEndpoint()).build(signer);
    }
}
