package dev.omatheusmesmo.demos.flocioci.expense;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The expense claim workflow: open a claim and hand out an upload URL, accept the submission once the receipt is in
 * the bucket, and serve the decrypted receipt to reviewers.
 */
@ApplicationScoped
public class ExpenseService {

    private final ReceiptBucket bucket;
    private final ReceiptJobs jobs;
    private final ReceiptCipher cipher;
    private final ExpensesConfig.Upload upload;

    public ExpenseService(ReceiptBucket bucket, ReceiptJobs jobs, ReceiptCipher cipher, ExpensesConfig config) {
        this.bucket = bucket;
        this.jobs = jobs;
        this.cipher = cipher;
        this.upload = config.upload();
    }

    public record Receipt(String contentType, byte[] content) {
    }

    @Transactional
    public OpenedExpense open(NewExpense request) {
        Expense expense = new Expense();
        expense.id = UUID.randomUUID();
        expense.employee = request.employee();
        expense.merchant = request.merchant();
        expense.amount = request.amount();
        expense.currency = request.currency();
        expense.spentOn = request.spentOn();
        expense.status = ExpenseStatus.AWAITING_RECEIPT;
        expense.createdAt = Instant.now();
        expense.persist();

        Instant expiresAt = expense.createdAt.plus(upload.expiry());
        String url = bucket.uploadUrl(expense.id, expiresAt);
        return new OpenedExpense(ExpenseView.of(expense), new OpenedExpense.Upload("PUT", url, expiresAt));
    }

    /**
     * Queues the receipt for processing. The job is enqueued inside the transaction, so a failed enqueue leaves the
     * claim open for another try.
     */
    @Transactional
    public ExpenseView submit(UUID id) {
        Expense expense = require(id);
        if (expense.status != ExpenseStatus.AWAITING_RECEIPT) {
            throw new ExpenseStateException("Expense " + id + " is already " + expense.status);
        }
        long size = bucket.size(ReceiptBucket.incoming(id))
                .orElseThrow(() -> new ExpenseStateException("No receipt has been uploaded for expense " + id));
        if (size > upload.maxBytes()) {
            throw new ReceiptTooLargeException(upload.maxBytes());
        }
        expense.status = ExpenseStatus.SUBMITTED;
        expense.submittedAt = Instant.now();
        jobs.enqueue(id);
        return ExpenseView.of(expense);
    }

    public ExpenseView find(UUID id) {
        return ExpenseView.of(require(id));
    }

    public List<ExpenseView> ofEmployee(String employee) {
        return Expense.ofEmployee(employee).stream().map(ExpenseView::of).toList();
    }

    public Receipt receipt(UUID id) {
        Expense expense = require(id);
        if (expense.status != ExpenseStatus.READY_FOR_REVIEW) {
            throw new ExpenseStateException("Expense " + id + " has no accepted receipt, it is " + expense.status);
        }
        byte[] content = cipher.open(bucket.read(expense.receiptObject), expense.receiptKey);
        if (!ReceiptProcessor.sha256(content).equals(expense.receiptSha256)) {
            throw new IllegalStateException("The receipt of expense " + id + " does not match its checksum");
        }
        return new Receipt(expense.receiptContentType, content);
    }

    private static Expense require(UUID id) {
        return Expense.<Expense>findByIdOptional(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
    }
}
