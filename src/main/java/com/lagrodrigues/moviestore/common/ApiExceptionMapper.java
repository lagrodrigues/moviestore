package com.lagrodrigues.moviestore.common;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Maps controlled application exceptions to a structured JSON response.
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    @Override
    public Response toResponse(ApiException exception) {
        Map<String, Object> body = Map.of(
                "timestamp", OffsetDateTime.now().toString(),
                "status", exception.getStatusCode(),
                "error", exception.getMessage()
        );

        return Response.status(exception.getStatusCode())
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}