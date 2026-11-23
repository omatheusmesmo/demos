package dev.omatheusmesmo.demos.flocioci.expense;

import com.oracle.bmc.keymanagement.KmsCryptoClient;
import com.oracle.bmc.keymanagement.model.DecryptDataDetails;
import com.oracle.bmc.keymanagement.model.GenerateKeyDetails;
import com.oracle.bmc.keymanagement.model.GeneratedKey;
import com.oracle.bmc.keymanagement.model.KeyShape;
import com.oracle.bmc.keymanagement.requests.DecryptRequest;
import com.oracle.bmc.keymanagement.requests.GenerateDataEncryptionKeyRequest;
import jakarta.enterprise.context.ApplicationScoped;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Envelope encryption of receipts. KMS generates a fresh AES-256 data key per receipt and returns it twice: in clear,
 * used here once and discarded, and wrapped by the master key, which is stored next to the record. The receipt never
 * goes to KMS (the encrypt API takes at most 4 KB), and the master key never leaves the vault.
 */
@ApplicationScoped
public class ReceiptCipher {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final KmsCryptoClient crypto;
    private final String masterKeyId;

    public ReceiptCipher(KmsCryptoClient crypto, ExpensesConfig config) {
        this.crypto = crypto;
        this.masterKeyId = config.keys().keyId();
    }

    public record Sealed(byte[] body, String wrappedKey) {
    }

    public Sealed seal(byte[] plaintext) {
        GeneratedKey dataKey = crypto.generateDataEncryptionKey(GenerateDataEncryptionKeyRequest.builder()
                        .generateKeyDetails(GenerateKeyDetails.builder()
                                .keyId(masterKeyId)
                                .includePlaintextKey(true)
                                .keyShape(KeyShape.builder().algorithm(KeyShape.Algorithm.Aes).length(32).build())
                                .build())
                        .build())
                .getGeneratedKey();
        byte[] key = Base64.getDecoder().decode(dataKey.getPlaintext());
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            byte[] ciphertext = aesGcm(Cipher.ENCRYPT_MODE, key, iv).doFinal(plaintext);
            byte[] body = ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
            return new Sealed(body, dataKey.getCiphertext());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot encrypt the receipt", e);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    public byte[] open(byte[] body, String wrappedKey) {
        String plaintextKey = crypto.decrypt(DecryptRequest.builder()
                        .decryptDataDetails(DecryptDataDetails.builder()
                                .keyId(masterKeyId)
                                .ciphertext(wrappedKey)
                                .build())
                        .build())
                .getDecryptedData()
                .getPlaintext();
        byte[] key = Base64.getDecoder().decode(plaintextKey);
        try {
            byte[] iv = Arrays.copyOfRange(body, 0, IV_BYTES);
            return aesGcm(Cipher.DECRYPT_MODE, key, iv).doFinal(body, IV_BYTES, body.length - IV_BYTES);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot decrypt the receipt", e);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    private static Cipher aesGcm(int mode, byte[] key, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
        return cipher;
    }
}
