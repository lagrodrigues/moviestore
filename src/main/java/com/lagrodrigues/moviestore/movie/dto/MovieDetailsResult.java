package com.lagrodrigues.moviestore.movie.dto;

/**
 * Represents a movie search result exposed by the internal API.
 */
public record MovieDetailsResult(
        String externalMovieId,
        String title,
        String year,
        String posterUrl,
        String imdbUrl,
        String actors
) {
}
