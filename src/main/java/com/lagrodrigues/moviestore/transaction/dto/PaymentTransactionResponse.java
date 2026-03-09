package com.lagrodrigues.moviestore.transaction.dto;

import com.lagrodrigues.moviestore.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a payment lifecycle transaction entry exposed through the API.
 */
public record PaymentTransactionResponse(
        UUID id,
        UUID paymentId,
        PaymentStatus fromStatus,
        PaymentStatus toStatus,
        BigDecimal amount,
        String message,
        OffsetDateTime createdAt
) {
}
