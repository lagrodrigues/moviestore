package com.lagrodrigues.moviestore.movie;

import com.lagrodrigues.moviestore.movie.dto.ExternalMovieSearchResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client used to communicate with the external movie service.
 */
@Path("/")
@RegisterRestClient(configKey = "movie-api")
@Produces(MediaType.APPLICATION_JSON)
public interface ExternalMovieClient {

    /**
     * Searches movies in the external catalogue.
     *
     * @param query the free-text search query
     * @return the external search response
     */
    @GET
    @Path("/search")
    ExternalMovieSearchResponse searchMovies(@QueryParam("q") String query);
}
