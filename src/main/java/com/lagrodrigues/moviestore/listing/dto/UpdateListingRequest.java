package com.lagrodrigues.moviestore.listing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

/**
 * Represents the payload required to update a store listing.
 *
 * <p>At least one of purchase price or rental price must remain configured.</p>
 */
public record UpdateListingRequest(

        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal purchasePrice,

        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal rentalPrice,

        @Min(0)
        Integer stock,

        Boolean active
) {
}
