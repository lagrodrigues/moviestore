package com.lagrodrigues.moviestore.listing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * Represents the payload required to create a store listing.
 *
 * <p>At least one of purchase price or rental price must be provided.</p>
 */
public record CreateListingRequest(

        @NotBlank
        String externalMovieId,

        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal purchasePrice,

        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal rentalPrice,

        @Min(0)
        Integer stock
) {
}
