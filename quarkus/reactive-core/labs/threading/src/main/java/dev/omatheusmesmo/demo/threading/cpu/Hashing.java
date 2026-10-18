package dev.omatheusmesmo.demo.threading.cpu;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * A fixed amount of CPU work: SHA-256 applied repeatedly to its own output.
 */
public final class Hashing {

    private Hashing() {
    }

    public static String digest(int rounds) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] value = "quarkus".getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < rounds; i++) {
                value = sha256.digest(value);
            }
            return HexFormat.of().formatHex(value);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is mandatory on every JVM", e);
        }
    }
}
