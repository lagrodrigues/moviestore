package com.lagrodrigues.moviestore.listing.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a store listing as exposed to customers when browsing a merchant's store.
 */
public record StoreBrowseResponse(
        UUID id,
        String externalMovieId,
        String title,
        String year,
        String posterUrl,
        BigDecimal purchasePrice,
        BigDecimal rentalPrice,
        Integer stock
) {
}
