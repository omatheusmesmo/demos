package dev.omatheusmesmo.demos.flocioci.expense;

import java.util.Arrays;
import java.util.Optional;

/**
 * Tells a receipt's type from its first bytes, not from what the uploader claims: the upload URL accepts any body.
 */
final class ReceiptFormat {

    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private ReceiptFormat() {
    }

    static Optional<String> detect(byte[] file) {
        if (startsWith(file, PDF)) {
            return Optional.of("application/pdf");
        }
        if (startsWith(file, PNG)) {
            return Optional.of("image/png");
        }
        if (startsWith(file, JPEG)) {
            return Optional.of("image/jpeg");
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] file, byte[] magic) {
        return file.length >= magic.length && Arrays.equals(file, 0, magic.length, magic, 0, magic.length);
    }
}
