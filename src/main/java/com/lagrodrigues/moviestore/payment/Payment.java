package com.lagrodrigues.moviestore.payment;

import com.lagrodrigues.moviestore.listing.StoreListing;
import com.lagrodrigues.moviestore.user.User;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_customer_idempotency", columnNames = {"customer_id", "idempotency_key"})
        }
)
public class Payment extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    public User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    public User merchant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_listing_id", nullable = false)
    public StoreListing storeListing;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    public String idempotencyKey;

    @Column(name = "external_movie_id", nullable = false, length = 50)
    public String externalMovieId;

    @Column(name = "movie_title", nullable = false, length = 255)
    public String movieTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public SaleType type;

    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public PaymentStatus status;

    @Column(name = "card_last4", length = 4)
    public String cardLast4;

    @Column(name = "failure_reason", length = 255)
    public String failureReason;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public OffsetDateTime updatedAt;

    @Version
    public long version;
}