package com.lagrodrigues.moviestore.payment.dto;

import com.lagrodrigues.moviestore.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a payment exposed through the API.
 */
public record PaymentResponse(
        UUID id,
        UUID listingId,
        UUID customerId,
        UUID merchantId,
        String externalMovieId,
        String movieTitle,
        String saleType,
        BigDecimal amount,
        PaymentStatus status,
        String failureReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
