package com.lagrodrigues.moviestore.common;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

/**
 * Central OpenAPI configuration for the application.
 *
 * <p>This Jakarta REST application class is used to expose application-level
 * OpenAPI metadata and shared security definitions.</p>
 */
@ApplicationPath("/")
@OpenAPIDefinition(
        info = @Info(
                title = "Movie Store API",
                version = "1.0.0",
                description = "Backend challenge implementation for a movie store platform.",
                contact = @Contact(name = "Luis Alfredo Rodrigues")
        ),
        security = @SecurityRequirement(name = "apiKey")
)
@SecurityScheme(
        securitySchemeName = "apiKey",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        apiKeyName = "X-API-KEY",
        description = "API key required to access secured endpoints."
)
public class OpenApiConfig extends Application {
}