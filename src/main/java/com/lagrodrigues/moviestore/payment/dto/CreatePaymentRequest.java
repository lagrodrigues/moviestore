package com.lagrodrigues.moviestore.payment.dto;

import com.lagrodrigues.moviestore.payment.SaleType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Represents the payload required to create a payment.
 */
public record CreatePaymentRequest(

        @NotNull
        UUID listingId,

        @NotNull
        SaleType saleType
) {
}
