package com.lagrodrigues.moviestore.auth;

import com.lagrodrigues.moviestore.user.User;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

/**
 * Provides simple endpoints to validate authentication behavior.
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "Endpoints related to request authentication.")
@SecurityRequirement(name = "apiKey")
public class AuthResource {

    @Inject
    AuthenticatedUser authenticatedUser;

    @Inject
    SecurityUtils securityUtils;

    /**
     * Returns basic information about the currently authenticated user.
     *
     * @return the current user's identity details
     */
    @GET
    @Path("/me")
    @Operation(
            summary = "Get current user",
            description = "Returns the identity of the currently authenticated user."
    )
    @APIResponse(responseCode = "200", description = "The authenticated user was returned successfully.")
    @APIResponse(responseCode = "401", description = "The request did not contain a valid API key.")
    public Map<String, Object> me() {
        User user = securityUtils.getCurrentUser(authenticatedUser);

        return Map.of(
                "id", user.id,
                "username", user.username,
                "role", user.role.name()
        );
    }
}