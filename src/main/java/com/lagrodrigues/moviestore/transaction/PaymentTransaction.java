package com.lagrodrigues.moviestore.transaction;

import com.lagrodrigues.moviestore.payment.Payment;
import com.lagrodrigues.moviestore.payment.PaymentStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents an immutable transaction history entry for a payment lifecycle event.
 */
@Entity
@Table(name = "payment_transaction")
public class PaymentTransaction extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    public Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 20)
    public PaymentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 50)
    public PaymentStatus toStatus;

    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal amount;

    @Column(length = 255)
    public String message;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;
}
