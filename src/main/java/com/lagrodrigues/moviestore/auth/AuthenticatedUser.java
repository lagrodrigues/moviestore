package com.lagrodrigues.moviestore.auth;

import com.lagrodrigues.moviestore.user.User;
import jakarta.enterprise.context.RequestScoped;

/**
 * Holds the authenticated user for the current HTTP request.
 *
 * <p>This bean is populated by the authentication filter once the API key
 * has been successfully validated.</p>
 */
@RequestScoped
public class AuthenticatedUser {

    private User user;

    /**
     * Returns the authenticated user associated with the current request.
     *
     * @return the authenticated user
     */
    public User getUser() {
        return user;
    }

    /**
     * Stores the authenticated user for the current request.
     *
     * @param user the authenticated user
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Indicates whether the current request has an authenticated user.
     *
     * @return {@code true} if a user is authenticated; {@code false} otherwise
     */
    public boolean isAuthenticated() {
        return user != null;
    }
}
