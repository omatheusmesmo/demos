package dev.omatheusmesmo.demos.invoicing.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReferenceCodecTest {

    @Test
    public void encodesTheInvoiceAndCustomerIntoAReference() {
        assertEquals("SU5WLTEtQUNNRQ==", ReferenceCodec.encode(1L, "ACME"));
    }

    @Test
    public void decodesAReferenceBackToItsPlainForm() {
        assertEquals("INV-42-GLOBEX", ReferenceCodec.decode(ReferenceCodec.encode(42L, "GLOBEX")));
    }
}
