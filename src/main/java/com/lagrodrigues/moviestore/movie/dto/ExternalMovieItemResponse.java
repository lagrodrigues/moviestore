package com.lagrodrigues.moviestore.movie.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single movie item returned by the external movie service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalMovieItemResponse(
        @JsonProperty("#IMDB_ID")
        String imdbId,

        @JsonProperty("#TITLE")
        String title,

        @JsonProperty("#YEAR")
        String year,

        @JsonProperty("#IMG_POSTER")
        String posterUrl,

        @JsonProperty("#IMDB_URL")
        String imdbUrl,

        @JsonProperty("#ACTORS")
        String actors
) {
}
