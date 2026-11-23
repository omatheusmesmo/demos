package dev.omatheusmesmo.demos.flocioci.oci;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Builds an OCI signer from an RSA key that exists only in memory. floci-oci reads the tenancy and user from the
 * signature's {@code keyId} and never checks the signature itself, so any key is accepted.
 */
public final class ThrowawayKey {

    private static final String FINGERPRINT = "aa:bb:cc:dd:ee:ff:00:11:22:33:44:55:66:77:88:99";

    private ThrowawayKey() {
    }

    public static SimpleAuthenticationDetailsProvider signer(String tenancyId, String userId, Region region) {
        byte[] pem = generatePem();
        return SimpleAuthenticationDetailsProvider.builder()
                .tenantId(tenancyId)
                .userId(userId)
                .fingerprint(FINGERPRINT)
                .privateKeySupplier(() -> new ByteArrayInputStream(pem))
                .region(region)
                .build();
    }

    private static byte[] generatePem() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                    .encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
            return ("-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n")
                    .getBytes(StandardCharsets.US_ASCII);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("The JVM has no RSA key generator", e);
        }
    }
}
