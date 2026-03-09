package com.lagrodrigues.moviestore.movie.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Represents the external response returned when searching for movies.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalMovieSearchResponse(
        List<ExternalMovieItemResponse> description
) {
}