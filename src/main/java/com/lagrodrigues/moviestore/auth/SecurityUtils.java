package com.lagrodrigues.moviestore.auth;

import com.lagrodrigues.moviestore.common.ApiException;
import com.lagrodrigues.moviestore.user.User;
import com.lagrodrigues.moviestore.user.UserRole;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Provides helper methods for validating the authenticated user's role.
 */
@ApplicationScoped
public class SecurityUtils {

    /**
     * Returns the currently authenticated user.
     *
     * @param authenticatedUser the request-scoped authenticated user context
     * @return the authenticated user
     */
    public User getCurrentUser(AuthenticatedUser authenticatedUser) {
        if (!authenticatedUser.isAuthenticated()) {
            throw new ApiException(401, "Authentication is required.");
        }
        return authenticatedUser.getUser();
    }

    /**
     * Ensures that the current user is a merchant.
     *
     * @param authenticatedUser the request-scoped authenticated user context
     * @return the authenticated merchant user
     */
    public User requireMerchant(AuthenticatedUser authenticatedUser) {
        User user = getCurrentUser(authenticatedUser);
        if (user.role != UserRole.MERCHANT) {
            throw new ApiException(403, "Merchant access is required.");
        }
        return user;
    }

    /**
     * Ensures that the current user is a customer.
     *
     * @param authenticatedUser the request-scoped authenticated user context
     * @return the authenticated customer user
     */
    public User requireCustomer(AuthenticatedUser authenticatedUser) {
        User user = getCurrentUser(authenticatedUser);
        if (user.role != UserRole.CUSTOMER) {
            throw new ApiException(403, "Customer access is required.");
        }
        return user;
    }
}