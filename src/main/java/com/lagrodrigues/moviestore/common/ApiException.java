package com.lagrodrigues.moviestore.common;

/**
 * Represents a controlled application exception that should be translated
 * into a meaningful HTTP response.
 */
public class ApiException extends RuntimeException {

    private final int statusCode;

    /**
     * Creates a new API exception.
     *
     * @param statusCode the HTTP status code to be returned
     * @param message the error message
     */
    public ApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Returns the HTTP status code associated with the exception.
     *
     * @return the HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }
}
