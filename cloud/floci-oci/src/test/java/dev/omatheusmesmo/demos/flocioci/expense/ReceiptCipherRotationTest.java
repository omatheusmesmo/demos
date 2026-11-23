package dev.omatheusmesmo.demos.flocioci.expense;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.keymanagement.KmsManagementClient;
import com.oracle.bmc.keymanagement.requests.CreateKeyVersionRequest;
import dev.omatheusmesmo.demos.flocioci.support.FlociOciResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Envelope encryption against floci-oci's real KMS: every receipt gets its own data key, and rotating the master key
 * does not strand the receipts encrypted before the rotation, because each wrapped data key names the key version
 * that wrapped it.
 */
@QuarkusTest
@WithTestResource(FlociOciResource.class)
class ReceiptCipherRotationTest {

    private static final byte[] RECEIPT = "%PDF-1.7 taxi receipt".getBytes(StandardCharsets.US_ASCII);

    @Inject
    ReceiptCipher cipher;

    @Inject
    ExpensesConfig config;

    @Inject
    AuthenticationDetailsProvider signer;

    @Test
    void opensAReceiptSealedBeforeTheMasterKeyWasRotated() {
        ReceiptCipher.Sealed beforeRotation = cipher.seal(RECEIPT);

        rotateMasterKey();
        ReceiptCipher.Sealed afterRotation = cipher.seal(RECEIPT);

        assertThat(cipher.open(beforeRotation.body(), beforeRotation.wrappedKey())).isEqualTo(RECEIPT);
        assertThat(cipher.open(afterRotation.body(), afterRotation.wrappedKey())).isEqualTo(RECEIPT);
    }

    @Test
    void sealsEveryReceiptWithItsOwnDataKey() {
        ReceiptCipher.Sealed first = cipher.seal(RECEIPT);
        ReceiptCipher.Sealed second = cipher.seal(RECEIPT);

        assertThat(first.wrappedKey()).isNotEqualTo(second.wrappedKey());
        assertThat(first.body()).isNotEqualTo(second.body());
    }

    /**
     * The management endpoint is the vault's, which floci-oci serves on the same host as the crypto endpoint.
     */
    private void rotateMasterKey() {
        try (KmsManagementClient keys = KmsManagementClient.builder()
                .endpoint(config.keys().cryptoEndpoint()).build(signer)) {
            keys.createKeyVersion(CreateKeyVersionRequest.builder().keyId(config.keys().keyId()).build());
        }
    }
}
