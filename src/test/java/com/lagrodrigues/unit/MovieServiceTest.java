package com.lagrodrigues.unit;

import com.lagrodrigues.moviestore.common.ApiException;
import com.lagrodrigues.moviestore.movie.ExternalMovieClient;
import com.lagrodrigues.moviestore.movie.MovieService;
import com.lagrodrigues.moviestore.movie.dto.ExternalMovieItemResponse;
import com.lagrodrigues.moviestore.movie.dto.ExternalMovieSearchResponse;
import com.lagrodrigues.moviestore.movie.dto.MovieDetailsResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MovieService}.
 */
@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    ExternalMovieClient externalMovieClient;

    @InjectMocks
    MovieService movieService;

    @Test
    void shouldSearchMoviesSuccessfully() {
        ExternalMovieItemResponse item = new ExternalMovieItemResponse(
                "tt0372784",
                "Batman Begins",
                "2005",
                "https://poster.example/batman.jpg",
                "https://imdb.com/title/tt0372784",
                "Christian Bale, Michael Caine"
        );

        ExternalMovieSearchResponse response = new ExternalMovieSearchResponse(List.of(item));

        when(externalMovieClient.searchMovies("batman")).thenReturn(response);

        List<MovieDetailsResult> result = movieService.searchMovies("batman");

        assertEquals(1, result.size());

        MovieDetailsResult movie = result.getFirst();
        assertEquals("tt0372784", movie.externalMovieId());
        assertEquals("Batman Begins", movie.title());
        assertEquals("2005", movie.year());
        assertEquals("https://poster.example/batman.jpg", movie.posterUrl());
        assertEquals("https://imdb.com/title/tt0372784", movie.imdbUrl());
        assertEquals("Christian Bale, Michael Caine", movie.actors());
    }

    @Test
    void shouldReturnEmptyListWhenSearchResponseDescriptionIsNull() {
        ExternalMovieSearchResponse response = new ExternalMovieSearchResponse(null);

        when(externalMovieClient.searchMovies("batman")).thenReturn(response);

        List<MovieDetailsResult> result = movieService.searchMovies("batman");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFailSearchWhenQueryIsBlank() {
        ApiException exception = assertThrows(ApiException.class, () ->
                movieService.searchMovies("   ")
        );

        assertEquals(400, exception.getStatusCode());
        assertEquals("Query must not be blank.", exception.getMessage());
    }

    @Test
    void shouldFailSearchWhenExternalServiceIsUnavailable() {
        when(externalMovieClient.searchMovies("batman"))
                .thenThrow(new RuntimeException("External service error"));

        ApiException exception = assertThrows(ApiException.class, () ->
                movieService.searchMovies("batman")
        );

        assertEquals(503, exception.getStatusCode());
        assertEquals("The external movie service is currently unavailable.", exception.getMessage());
    }

    @Test
    void shouldGetMovieByIdSuccessfully() {
        ExternalMovieItemResponse item = new ExternalMovieItemResponse(
                "tt0372784",
                "Batman Begins",
                "2005",
                "https://poster.example/batman.jpg",
                "https://imdb.com/title/tt0372784",
                "Christian Bale, Michael Caine"
        );

        ExternalMovieSearchResponse response = new ExternalMovieSearchResponse(List.of(item));

        when(externalMovieClient.searchMovies("tt0372784")).thenReturn(response);

        MovieDetailsResult result = movieService.getMovieById("tt0372784");

        assertNotNull(result);
        assertEquals("tt0372784", result.externalMovieId());
        assertEquals("Batman Begins", result.title());
        assertEquals("2005", result.year());
        assertEquals("https://poster.example/batman.jpg", result.posterUrl());
        assertEquals("https://imdb.com/title/tt0372784", result.imdbUrl());
        assertEquals("Christian Bale, Michael Caine", result.actors());
    }

    @Test
    void shouldFailGetMovieByIdWhenIdIsBlank() {
        ApiException exception = assertThrows(ApiException.class, () ->
                movieService.getMovieById(" ")
        );

        assertEquals(400, exception.getStatusCode());
        assertEquals("External movie ID must not be blank.", exception.getMessage());
    }

    @Test
    void shouldFailGetMovieByIdWhenMovieIsNotFound() {
        ExternalMovieSearchResponse response = new ExternalMovieSearchResponse(List.of());

        when(externalMovieClient.searchMovies("tt0372784")).thenReturn(response);

        ApiException exception = assertThrows(ApiException.class, () ->
                movieService.getMovieById("tt0372784")
        );

        assertEquals(404, exception.getStatusCode());
        assertEquals("Movie not found in the external catalogue.", exception.getMessage());
    }

    @Test
    void shouldFailGetMovieByIdWhenExternalServiceIsUnavailable() {
        when(externalMovieClient.searchMovies("tt0372784"))
                .thenThrow(new RuntimeException("External service error"));

        ApiException exception = assertThrows(ApiException.class, () ->
                movieService.getMovieById("tt0372784")
        );

        assertEquals(503, exception.getStatusCode());
        assertEquals("The external movie service is currently unavailable.", exception.getMessage());
    }
}