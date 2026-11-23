package dev.omatheusmesmo.demos.flocioci.expense;

/**
 * The request does not fit the expense's current state: submitting twice, submitting before the upload, downloading
 * a receipt that was not accepted.
 */
public class ExpenseStateException extends RuntimeException {

    public ExpenseStateException(String message) {
        super(message);
    }
}
