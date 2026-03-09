package com.lagrodrigues.moviestore.listing;

import com.lagrodrigues.moviestore.auth.AuthenticatedUser;
import com.lagrodrigues.moviestore.auth.SecurityUtils;
import com.lagrodrigues.moviestore.listing.dto.CreateListingRequest;
import com.lagrodrigues.moviestore.listing.dto.ListingResponse;
import com.lagrodrigues.moviestore.listing.dto.UpdateListingRequest;
import com.lagrodrigues.moviestore.user.User;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

/**
 * Exposes endpoints related to merchant store listings.
 */
@Path("/api/merchant/listings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Merchant Listings", description = "Operations related to merchant store listings.")
@SecurityRequirement(name = "apiKey")
public class StoreListingResource {

    @Inject
    AuthenticatedUser authenticatedUser;

    @Inject
    SecurityUtils securityUtils;

    @Inject
    StoreListingService service;

    /**
     * Creates a new store listing.
     *
     * @param request the listing creation request
     * @return the created listing
     */
    @POST
    @Operation(
            summary = "Create store listing",
            description = "Creates a merchant store listing by referencing a movie from the external catalogue."
    )
    @APIResponse(responseCode = "200", description = "Listing created successfully.")
    @APIResponse(responseCode = "400", description = "The request is invalid.")
    @APIResponse(responseCode = "401", description = "The request did not contain a valid API key.")
    @APIResponse(responseCode = "403", description = "Merchant access is required.")
    @APIResponse(responseCode = "503", description = "The external movie service is unavailable.")
    public ListingResponse createListing(@Valid CreateListingRequest request) {
        User merchant = securityUtils.requireMerchant(authenticatedUser);
        StoreListing listing = service.createListing(merchant, request);
        return toResponse(listing);
    }

    /**
     * Returns all listings belonging to the authenticated merchant.
     *
     * @return the merchant's listings
     */
    @GET
    @Operation(
            summary = "Get merchant listings",
            description = "Returns all listings belonging to the authenticated merchant."
    )
    @APIResponse(responseCode = "200", description = "Listings returned successfully.")
    @APIResponse(responseCode = "401", description = "The request did not contain a valid API key.")
    @APIResponse(responseCode = "403", description = "Merchant access is required.")
    public List<ListingResponse> getMerchantListings() {
        User merchant = securityUtils.requireMerchant(authenticatedUser);

        return service.getMerchantListings(merchant).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns a specific listing belonging to the authenticated merchant.
     *
     * @param listingId the listing identifier
     * @return the requested listing
     */
    @GET
    @Path("/{listingId}")
    @Operation(
            summary = "Get merchant listing by ID",
            description = "Returns a specific listing belonging to the authenticated merchant."
    )
    @APIResponse(responseCode = "200", description = "Listing returned successfully.")
    @APIResponse(responseCode = "401", description = "The request did not contain a valid API key.")
    @APIResponse(responseCode = "403", description = "Merchant access is required.")
    @APIResponse(responseCode = "404", description = "Listing not found.")
    public ListingResponse getMerchantListingById(@PathParam("listingId") UUID listingId) {
        User merchant = securityUtils.requireMerchant(authenticatedUser);
        StoreListing listing = service.getMerchantListingById(merchant, listingId);
        return toResponse(listing);
    }

    /**
     * Updates an existing listing owned by the authenticated merchant.
     *
     * @param listingId the listing identifier
     * @param request the listing update request
     * @return the updated listing
     */
    @PUT
    @Path("/{listingId}")
    @Operation(
            summary = "Update store listing",
            description = "Updates an existing store listing owned by the authenticated merchant."
    )
    @APIResponse(responseCode = "200", description = "Listing updated successfully.")
    @APIResponse(responseCode = "400", description = "The request is invalid.")
    @APIResponse(responseCode = "401", description = "The request did not contain a valid API key.")
    @APIResponse(responseCode = "403", description = "Merchant access is required.")
    @APIResponse(responseCode = "404", description = "Listing not found.")
    public ListingResponse updateListing(
            @PathParam("listingId") UUID listingId,
            @Valid UpdateListingRequest request) {

        User merchant = securityUtils.requireMerchant(authenticatedUser);
        StoreListing listing = service.updateListing(merchant, listingId, request);
        return toResponse(listing);
    }

    /**
     * Maps the entity to the API response model.
     *
     * @param listing the listing entity
     * @return the mapped response
     */
    private ListingResponse toResponse(StoreListing listing) {
        return new ListingResponse(
                listing.id,
                listing.externalMovieId,
                listing.movieTitle,
                listing.movieYear,
                listing.moviePosterUrl,
                listing.purchasePrice,
                listing.rentalPrice,
                listing.stock,
                listing.active,
                listing.createdAt,
                listing.updatedAt
        );
    }
}