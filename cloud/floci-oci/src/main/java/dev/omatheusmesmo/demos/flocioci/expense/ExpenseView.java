package dev.omatheusmesmo.demos.flocioci.expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * What the API returns for an expense: never the object name or the wrapped data key of the receipt.
 */
public record ExpenseView(UUID id, String employee, String merchant, BigDecimal amount, String currency,
                          LocalDate spentOn, ExpenseStatus status, String rejectionReason, String receiptContentType,
                          Long receiptSize, Instant createdAt, Instant processedAt) {

    static ExpenseView of(Expense expense) {
        return new ExpenseView(expense.id, expense.employee, expense.merchant, expense.amount, expense.currency,
                expense.spentOn, expense.status, expense.rejectionReason, expense.receiptContentType,
                expense.receiptSize, expense.createdAt, expense.processedAt);
    }
}
