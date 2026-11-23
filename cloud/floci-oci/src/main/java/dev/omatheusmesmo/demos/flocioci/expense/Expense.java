package dev.omatheusmesmo.demos.flocioci.expense;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * An expense claim as PostgreSQL stores it. The receipt itself is in Object Storage, encrypted; the row keeps where it
 * is, its checksum and the data key that encrypted it, wrapped by the KMS master key.
 */
@Entity
@Table(name = "expenses")
public class Expense extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false, length = 100)
    public String employee;

    @Column(nullable = false, length = 200)
    public String merchant;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal amount;

    @Column(nullable = false, length = 3)
    public String currency;

    @Column(name = "spent_on", nullable = false)
    public LocalDate spentOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public ExpenseStatus status;

    @Column(name = "rejection_reason", length = 500)
    public String rejectionReason;

    @Column(name = "receipt_content_type", length = 100)
    public String receiptContentType;

    @Column(name = "receipt_size")
    public Long receiptSize;

    @Column(name = "receipt_sha256", length = 64)
    public String receiptSha256;

    @Column(name = "receipt_object", length = 200)
    public String receiptObject;

    @Column(name = "receipt_key", columnDefinition = "text")
    public String receiptKey;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "submitted_at")
    public Instant submittedAt;

    @Column(name = "processed_at")
    public Instant processedAt;

    public static List<Expense> ofEmployee(String employee) {
        return list("employee", Sort.by("createdAt"), employee);
    }
}
