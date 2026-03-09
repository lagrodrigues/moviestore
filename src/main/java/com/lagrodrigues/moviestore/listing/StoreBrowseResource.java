package com.lagrodrigues.moviestore.listing;

import com.lagrodrigues.moviestore.auth.AuthenticatedUser;
import com.lagrodrigues.moviestore.auth.SecurityUtils;
import com.lagrodrigues.moviestore.listing.dto.StoreBrowseResponse;
import com.lagrodrigues.moviestore.listing.dto.StoreListingDetailsResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

/**
 * Exposes customer-facing endpoints for browsing merchant stores.
 */
@Path("/api/stores")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Stores", description = "Customer-facing operations for browsing merchant stores.")
@SecurityRequirement(name = "apiKey")
public class StoreBrowseResource {

    @Inject
    AuthenticatedUser authenticatedUser;

    @Inject
    SecurityUtils securityUtils;

    @Inject
    StoreListingService storeListingService;

    /**
     * Returns the active listings available in a merchant's store.
     *
     * @param merchantId the merchant identifier
     * @return the merchant's active listings
     */
    @GET
    @Path("/{merchantId}/listings")
    @Operation(
            summary = "Browse merchant store",
            description = "Returns the active listings available in the specified merchant's store."
    )
    @APIResponse(responseCode = "200", description = "Store listings returned successfully.")
    @APIResponse(responseCode = "401", description = "The request did not contain a valid API key.")
    @APIResponse(responseCode = "403", description = "Customer access is required.")
    public List<StoreBrowseResponse> browseMerchantStore(@PathParam("merchantId") UUID merchantId) {
        securityUtils.requireCustomer(authenticatedUser);

        return storeListingService.browseMerchantStore(merchantId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns the details of a specific active listing in a merchant's store.
     *
     * @param merchantId the merchant identifier
     * @param listingId  the listing identifier
     * @return the requested store listing details
     */
    @GET
    @Path("/{merchantId}/listings/{listingId}")
    @Operation(
            summary = "Get store listing details",
            description = "Returns the details of a specific active listing in the specified merchant's store."
    )
    @APIResponse(responseCode = "200", description = "Store listing details returned successfully.")
    @APIResponse(responseCode = "401", description = "The request did not contain a valid API key.")
    @APIResponse(responseCode = "403", description = "Customer access is required.")
    @APIResponse(responseCode = "404", description = "Store listing not found.")
    public StoreListingDetailsResponse getStoreListingDetails(
            @PathParam("merchantId") UUID merchantId,
            @PathParam("listingId") UUID listingId) {

        securityUtils.requireCustomer(authenticatedUser);

        StoreListing listing = storeListingService.getStoreListing(merchantId, listingId);

        return new StoreListingDetailsResponse(
                listing.id,
                listing.externalMovieId,
                listing.movieTitle,
                listing.movieYear,
                listing.moviePosterUrl,
                listing.purchasePrice,
                listing.rentalPrice,
                listing.stock,
                listing.stock != null && listing.stock > 0
        );
    }

    /**
     * Maps the listing entity to the customer-facing store response.
     *
     * @param listing the listing entity
     * @return the mapped response
     */
    private StoreBrowseResponse toResponse(StoreListing listing) {
        return new StoreBrowseResponse(
                listing.id,
                listing.externalMovieId,
                listing.movieTitle,
                listing.movieYear,
                listing.moviePosterUrl,
                listing.purchasePrice,
                listing.rentalPrice,
                listing.stock
        );
    }
}
