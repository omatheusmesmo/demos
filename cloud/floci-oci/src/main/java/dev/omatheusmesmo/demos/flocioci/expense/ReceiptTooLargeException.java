package dev.omatheusmesmo.demos.flocioci.expense;

public class ReceiptTooLargeException extends RuntimeException {

    public ReceiptTooLargeException(long maxBytes) {
        super("The receipt is larger than " + maxBytes + " bytes");
    }
}
