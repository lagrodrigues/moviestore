package com.lagrodrigues.moviestore.auth;

import com.lagrodrigues.moviestore.common.ApiException;
import com.lagrodrigues.moviestore.user.User;
import com.lagrodrigues.moviestore.user.UserRepository;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

import java.util.Optional;

/**
 * Authenticates incoming requests using a simple API key mechanism.
 *
 * <p>The API key must be supplied in the {@code X-API-KEY} HTTP header.
 * Once validated, the corresponding user is stored in the request-scoped
 * authenticated user context.</p>
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";

    @Inject
    UserRepository userRepository;

    @Inject
    AuthenticatedUser authenticatedUser;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();

        if (isPublicPath(path)) {
            return;
        }

        String apiKey = requestContext.getHeaderString(API_KEY_HEADER);

        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(401, "Missing API key.");
        }

        Optional<User> userOptional = userRepository.findByApiKey(apiKey);

        if (userOptional.isEmpty()) {
            throw new ApiException(401, "Invalid API key.");
        }

        authenticatedUser.setUser(userOptional.get());
    }

    /**
     * Determines whether the requested path should bypass authentication.
     *
     * @param path the request path
     * @return {@code true} if the path is public; {@code false} otherwise
     */
    private boolean isPublicPath(String path) {
        return path.startsWith("swagger")
                || path.startsWith("openapi")
                || path.startsWith("q");
    }
}
