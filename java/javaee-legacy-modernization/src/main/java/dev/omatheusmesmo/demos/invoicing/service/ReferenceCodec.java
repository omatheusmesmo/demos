package dev.omatheusmesmo.demos.invoicing.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public final class ReferenceCodec {

    private ReferenceCodec() {
    }

    public static String encode(Long invoiceId, String customerCode) {
        String plain = "INV-" + invoiceId + "-" + customerCode;
        return new BASE64Encoder().encode(plain.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String reference) {
        try {
            return new String(new BASE64Decoder().decodeBuffer(reference), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Not a valid invoice reference: " + reference, e);
        }
    }
}
