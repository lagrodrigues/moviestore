package com.lagrodrigues.moviestore.transaction.dto;

import java.util.List;

/**
 * Represents a generic paged API response.
 *
 * @param items the page content
 * @param page the current page index, zero-based
 * @param size the page size
 * @param totalItems the total number of matching items
 * @param totalPages the total number of available pages
 */
public record PagedResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}