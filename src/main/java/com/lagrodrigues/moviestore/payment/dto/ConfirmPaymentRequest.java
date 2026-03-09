package com.lagrodrigues.moviestore.payment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Represents the payload required to confirm a payment using simulated card details.
 */
public record ConfirmPaymentRequest(

        @NotBlank
        @Pattern(regexp = "\\d{13,19}")
        String cardNumber,

        @NotBlank
        String cardHolderName,

        @NotNull
        @Min(1)
        @Max(12)
        Integer expiryMonth,

        @NotNull
        @Min(2024)
        @Max(2100)
        Integer expiryYear,

        @NotBlank
        @Pattern(regexp = "\\d{3,4}")
        String cvv
) {
}
