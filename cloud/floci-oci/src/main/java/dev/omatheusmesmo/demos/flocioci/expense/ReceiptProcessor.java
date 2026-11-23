package dev.omatheusmesmo.demos.flocioci.expense;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles one receipt job. Every step can run again safely, because the queue delivers a job again whenever the
 * previous attempt did not finish: a job for an expense that is no longer {@code SUBMITTED} is a no-op, and the
 * uploaded file is only deleted once the record points at the encrypted copy.
 */
@ApplicationScoped
public class ReceiptProcessor {

    private final ReceiptBucket bucket;
    private final ReceiptCipher cipher;
    private final long maxBytes;

    public ReceiptProcessor(ReceiptBucket bucket, ReceiptCipher cipher, ExpensesConfig config) {
        this.bucket = bucket;
        this.cipher = cipher;
        this.maxBytes = config.upload().maxBytes();
    }

    public void process(UUID expenseId) {
        Expense expense = QuarkusTransaction.requiringNew().call(() -> Expense.<Expense>findById(expenseId));
        if (expense == null) {
            throw new IllegalStateException("No expense " + expenseId);
        }
        if (expense.status != ExpenseStatus.SUBMITTED) {
            return;
        }

        byte[] file = bucket.read(ReceiptBucket.incoming(expenseId));
        Optional<String> contentType = ReceiptFormat.detect(file);
        if (file.length > maxBytes) {
            reject(expenseId, "The receipt is larger than " + maxBytes + " bytes");
        } else if (contentType.isEmpty()) {
            reject(expenseId, "The receipt is not a PDF, PNG or JPEG file");
        } else {
            ReceiptCipher.Sealed sealed = cipher.seal(file);
            bucket.write(ReceiptBucket.stored(expenseId), sealed.body());
            accept(expenseId, contentType.get(), file, sealed.wrappedKey());
        }
        bucket.delete(ReceiptBucket.incoming(expenseId));
    }

    private void accept(UUID expenseId, String contentType, byte[] file, String wrappedKey) {
        QuarkusTransaction.requiringNew().run(() -> {
            Expense expense = Expense.findById(expenseId);
            expense.status = ExpenseStatus.READY_FOR_REVIEW;
            expense.receiptContentType = contentType;
            expense.receiptSize = (long) file.length;
            expense.receiptSha256 = sha256(file);
            expense.receiptObject = ReceiptBucket.stored(expenseId);
            expense.receiptKey = wrappedKey;
            expense.processedAt = Instant.now();
        });
    }

    private void reject(UUID expenseId, String reason) {
        QuarkusTransaction.requiringNew().run(() -> {
            Expense expense = Expense.findById(expenseId);
            expense.status = ExpenseStatus.REJECTED;
            expense.rejectionReason = reason;
            expense.processedAt = Instant.now();
        });
    }

    static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
