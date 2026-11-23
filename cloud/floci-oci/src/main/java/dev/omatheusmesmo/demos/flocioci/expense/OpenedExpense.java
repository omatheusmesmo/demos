package dev.omatheusmesmo.demos.flocioci.expense;

import java.time.Instant;

/**
 * A new expense and where to upload its receipt: a pre-authenticated request URL that accepts one PUT of one object
 * and needs no OCI credentials.
 */
public record OpenedExpense(ExpenseView expense, Upload upload) {

    public record Upload(String method, String url, Instant expiresAt) {
    }
}
