package com.lagrodrigues.moviestore.listing.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents the details of a store listing as exposed to customers.
 */
public record StoreListingDetailsResponse(
        UUID id,
        String externalMovieId,
        String title,
        String year,
        String posterUrl,
        BigDecimal purchasePrice,
        BigDecimal rentalPrice,
        Integer stock,
        Boolean available
) {
}