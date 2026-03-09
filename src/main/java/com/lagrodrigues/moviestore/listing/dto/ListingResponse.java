package com.lagrodrigues.moviestore.listing.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a store listing exposed through the API.
 */
public record ListingResponse(
        UUID id,
        String externalMovieId,
        String title,
        String year,
        String posterUrl,
        BigDecimal purchasePrice,
        BigDecimal rentalPrice,
        Integer stock,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
