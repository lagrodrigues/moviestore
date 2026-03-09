package com.lagrodrigues.moviestore.listing;

import com.lagrodrigues.moviestore.user.User;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a movie listing published by a merchant in the local store.
 *
 * <p>A listing references a movie owned by the external movie service and stores
 * a local snapshot of the essential movie information required by the platform.</p>
 *
 * <p>A single listing may support purchase, rental, or both, depending on which
 * prices are configured.</p>
 */
@Entity
@Table(name = "store_listing")
public class StoreListing extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    public User merchant;

    @Column(name = "external_movie_id", nullable = false, length = 50)
    public String externalMovieId;

    @Column(name = "movie_title", nullable = false, length = 255)
    public String movieTitle;

    @Column(name = "movie_year", length = 10)
    public String movieYear;

    @Column(name = "movie_poster_url", length = 500)
    public String moviePosterUrl;

    @Column(name = "purchase_price", precision = 10, scale = 2)
    public BigDecimal purchasePrice;

    @Column(name = "rental_price", precision = 10, scale = 2)
    public BigDecimal rentalPrice;

    @Column(nullable = false)
    public Integer stock;

    @Column(nullable = false)
    public Boolean active;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public OffsetDateTime updatedAt;

    @Version
    public long version;
}
