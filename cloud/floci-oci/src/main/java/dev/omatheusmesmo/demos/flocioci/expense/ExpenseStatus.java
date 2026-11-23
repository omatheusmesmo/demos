package dev.omatheusmesmo.demos.flocioci.expense;

/**
 * The life of an expense claim: the employee opens it, uploads the receipt and submits it; the receipt worker then
 * either accepts the receipt for review or rejects it.
 */
public enum ExpenseStatus {
    AWAITING_RECEIPT,
    SUBMITTED,
    READY_FOR_REVIEW,
    REJECTED
}
