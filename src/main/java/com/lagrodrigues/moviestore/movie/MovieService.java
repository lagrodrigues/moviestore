package com.lagrodrigues.moviestore.movie;

import com.lagrodrigues.moviestore.common.ApiException;
import com.lagrodrigues.moviestore.movie.dto.ExternalMovieItemResponse;
import com.lagrodrigues.moviestore.movie.dto.ExternalMovieSearchResponse;
import com.lagrodrigues.moviestore.movie.dto.MovieDetailsResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Collections;
import java.util.List;

/**
 * Provides movie-related application services.
 */
@ApplicationScoped
public class MovieService {

    @Inject
    @RestClient
    ExternalMovieClient externalMovieClient;

    /**
     * Searches movies using the external movie service and maps the result to the internal API model.
     *
     * @param query the free-text search query
     * @return a list of mapped movie search results
     */
    @Timeout(3000)
    @Retry(maxRetries = 2, delay = 200)
    public List<MovieDetailsResult> searchMovies(String query) {
        if (query == null || query.isBlank()) {
            throw new ApiException(400, "Query must not be blank.");
        }

        try {
            ExternalMovieSearchResponse response = externalMovieClient.searchMovies(query);

            if (response == null || response.description() == null) {
                return Collections.emptyList();
            }

            return response.description().stream()
                    .map(this::mapToResult)
                    .toList();
        } catch (Exception exception) {
            throw new ApiException(503, "The external movie service is currently unavailable.");
        }
    }

    /**
     * Retrieves detailed movie information using the external movie identifier.
     *
     * @param externalMovieId the external IMDb identifier
     * @return the mapped movie details
     */
    @Timeout(3000)
    @Retry(maxRetries = 2, delay = 200)
    public MovieDetailsResult getMovieById(String externalMovieId) {
        if (externalMovieId == null || externalMovieId.isBlank()) {
            throw new ApiException(400, "External movie ID must not be blank.");
        }

        try {
            ExternalMovieSearchResponse response = externalMovieClient.searchMovies(externalMovieId);

            if (response == null || response.description() == null || response.description().isEmpty()) {
                throw new ApiException(404, "Movie not found in the external catalogue.");
            }

            ExternalMovieItemResponse movie = response.description().getFirst();

            if (movie.imdbId() == null || movie.imdbId().isBlank()) {
                throw new ApiException(404, "Movie not found in the external catalogue.");
            }

            return mapToResult(movie);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(503, "The external movie service is currently unavailable.");
        }
    }

    /**
     * Maps the external movie item to the internal API result model.
     *
     * @param item the external movie item
     * @return the mapped internal movie result
     */
    private MovieDetailsResult mapToResult(ExternalMovieItemResponse item) {
        return new MovieDetailsResult(
                item.imdbId(),
                item.title(),
                item.year(),
                item.posterUrl(),
                item.imdbUrl(),
                item.actors()
        );
    }
}
