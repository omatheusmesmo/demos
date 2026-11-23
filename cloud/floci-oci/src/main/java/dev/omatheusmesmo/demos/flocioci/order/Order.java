package dev.omatheusmesmo.demos.flocioci.order;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * An order as Oracle Database stores it. The card number never reaches the database in clear: only its last four
 * digits and the ciphertext OCI KMS returned.
 */
@Entity
@Table(name = "customer_orders")
public class Order extends PanacheEntity {

    @Column(nullable = false)
    public String customer;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal total;

    @Column(name = "card_last4", nullable = false, length = 4)
    public String cardLast4;

    @Column(name = "card_ciphertext", nullable = false, length = 4000)
    public String cardCiphertext;

    @Column(name = "receipt_object")
    public String receiptObject;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
