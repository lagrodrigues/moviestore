package com.lagrodrigues.moviestore.movie;

import com.lagrodrigues.moviestore.auth.AuthenticatedUser;
import com.lagrodrigues.moviestore.auth.SecurityUtils;
import com.lagrodrigues.moviestore.movie.dto.MovieDetailsResult;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/**
 * Exposes movie catalogue operations backed by an external movie service.
 */
@Path("/api/movies")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Movies", description = "Operations related to the external movie catalogue.")
@SecurityRequirement(name = "apiKey")
public class MovieResource {

    @Inject
    AuthenticatedUser authenticatedUser;

    @Inject
    SecurityUtils securityUtils;

    @Inject
    MovieService movieService;

    /**
     * Searches movies in the external catalogue.
     *
     * <p>This operation is restricted to merchants, as it is intended to support
     * the creation of store listings from external movie references.</p>
     *
     * @param query the free-text search query
     * @return the list of movie search results
     */
    @GET
    @Path("/search")
    @Operation(
            summary = "Search movies",
            description = "Searches movies using the external movie service."
    )
    @APIResponse(responseCode = "200", description = "Movie results returned successfully.")
    @APIResponse(responseCode = "400", description = "The query parameter is invalid.")
    @APIResponse(responseCode = "401", description = "The request did not contain a valid API key.")
    @APIResponse(responseCode = "403", description = "Merchant access is required.")
    @APIResponse(responseCode = "503", description = "The external movie service is unavailable.")
    public List<MovieDetailsResult> searchMovies(@QueryParam("query") String query) {
        securityUtils.requireMerchant(authenticatedUser);
        return movieService.searchMovies(query);
    }
}
