package dev.omatheusmesmo.demos.flocioci.order;

import com.oracle.bmc.keymanagement.KmsCryptoClient;
import com.oracle.bmc.keymanagement.model.DecryptDataDetails;
import com.oracle.bmc.keymanagement.model.EncryptDataDetails;
import com.oracle.bmc.keymanagement.requests.DecryptRequest;
import com.oracle.bmc.keymanagement.requests.EncryptRequest;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Encrypts card numbers with an AES key held by OCI KMS. The key material never leaves the vault: the application
 * only ever sees plaintext and ciphertext, both base64 encoded as the KMS API requires.
 */
@ApplicationScoped
public class CardVault {

    private final KmsCryptoClient crypto;
    private final String keyId;

    public CardVault(KmsCryptoClient crypto, OrdersConfig config) {
        this.crypto = crypto;
        this.keyId = config.cards().keyId();
    }

    public String encrypt(String cardNumber) {
        return crypto.encrypt(EncryptRequest.builder()
                        .encryptDataDetails(EncryptDataDetails.builder()
                                .keyId(keyId)
                                .plaintext(Base64.getEncoder().encodeToString(cardNumber.getBytes(StandardCharsets.UTF_8)))
                                .build())
                        .build())
                .getEncryptedData()
                .getCiphertext();
    }

    public String decrypt(String ciphertext) {
        String plaintext = crypto.decrypt(DecryptRequest.builder()
                        .decryptDataDetails(DecryptDataDetails.builder()
                                .keyId(keyId)
                                .ciphertext(ciphertext)
                                .build())
                        .build())
                .getDecryptedData()
                .getPlaintext();
        return new String(Base64.getDecoder().decode(plaintext), StandardCharsets.UTF_8);
    }
}
