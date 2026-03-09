package com.lagrodrigues.moviestore.listing;

import com.lagrodrigues.moviestore.common.ApiException;
import com.lagrodrigues.moviestore.listing.dto.CreateListingRequest;
import com.lagrodrigues.moviestore.listing.dto.UpdateListingRequest;
import com.lagrodrigues.moviestore.movie.MovieService;
import com.lagrodrigues.moviestore.movie.dto.MovieDetailsResult;
import com.lagrodrigues.moviestore.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Provides application services related to merchant store listings.
 */
@ApplicationScoped
public class StoreListingService {

    @Inject
    StoreListingRepository repository;

    @Inject
    MovieService movieService;

    /**
     * Creates a new store listing for the given merchant.
     *
     * <p>The merchant references the movie by its external identifier. The service
     * then retrieves the official movie details from the external catalogue and stores
     * a local snapshot in the listing.</p>
     *
     * @param merchant the authenticated merchant
     * @param request the listing creation request
     * @return the persisted listing
     */
    @Transactional
    public StoreListing createListing(User merchant, CreateListingRequest request) {
        validateCreateRequest(request);

        MovieDetailsResult movie = movieService.getMovieById(request.externalMovieId());

        StoreListing listing = new StoreListing();
        listing.merchant = merchant;
        listing.externalMovieId = movie.externalMovieId();
        listing.movieTitle = movie.title();
        listing.movieYear = movie.year();
        listing.moviePosterUrl = movie.posterUrl();
        listing.purchasePrice = request.purchasePrice();
        listing.rentalPrice = request.rentalPrice();
        listing.stock = request.stock();
        listing.active = true;
        listing.createdAt = OffsetDateTime.now();
        listing.updatedAt = OffsetDateTime.now();

        repository.persist(listing);

        return listing;
    }

    /**
     * Returns all listings belonging to the given merchant.
     *
     * @param merchant the authenticated merchant
     * @return the merchant's listings
     */
    public List<StoreListing> getMerchantListings(User merchant) {
        return repository.findByMerchantId(merchant.id);
    }

    /**
     * Returns a specific listing if it belongs to the given merchant.
     *
     * @param merchant the authenticated merchant
     * @param listingId the listing identifier
     * @return the matching listing
     */
    public StoreListing getMerchantListingById(User merchant, UUID listingId) {
        return repository.findByIdAndMerchantId(listingId, merchant.id)
                .orElseThrow(() -> new ApiException(404, "Listing not found."));
    }

    /**
     * Updates an existing listing owned by the given merchant.
     *
     * @param merchant the authenticated merchant
     * @param listingId the listing identifier
     * @param request the update request
     * @return the updated listing
     */
    @Transactional
    public StoreListing updateListing(User merchant, UUID listingId, UpdateListingRequest request) {
        StoreListing listing = getMerchantListingById(merchant, listingId);

        if (request.purchasePrice() != null) {
            listing.purchasePrice = request.purchasePrice();
        }

        if (request.rentalPrice() != null) {
            listing.rentalPrice = request.rentalPrice();
        }

        if (request.stock() != null) {
            listing.stock = request.stock();
        }

        if (request.active() != null) {
            listing.active = request.active();
        }

        validateFinalListingState(listing);

        listing.updatedAt = OffsetDateTime.now();

        return listing;
    }

    /**
     * Validates the listing creation request.
     *
     * @param request the request to validate
     */
    private void validateCreateRequest(CreateListingRequest request) {
        if (request.purchasePrice() == null && request.rentalPrice() == null) {
            throw new ApiException(400, "At least one of purchasePrice or rentalPrice must be provided.");
        }

        if (request.stock() == null) {
            throw new ApiException(400, "Stock must be provided.");
        }

        if (request.stock() < 0) {
            throw new ApiException(400, "Stock must be greater than or equal to zero.");
        }
    }

    /**
     * Validates the final state of a listing after an update.
     *
     * @param listing the listing to validate
     */
    private void validateFinalListingState(StoreListing listing) {
        if (listing.purchasePrice == null && listing.rentalPrice == null) {
            throw new ApiException(400, "At least one of purchasePrice or rentalPrice must be configured.");
        }

        if (listing.stock == null || listing.stock < 0) {
            throw new ApiException(400, "Stock must be greater than or equal to zero.");
        }
    }

    /**
     * Returns the active listings available in the given merchant's store.
     *
     * @param merchantId the merchant identifier
     * @return the merchant's active store listings
     */
    public List<StoreListing> browseMerchantStore(UUID merchantId) {
        return repository.findActiveByMerchantId(merchantId);
    }

    /**
     * Returns a specific active listing from the given merchant's store.
     *
     * @param merchantId the merchant identifier
     * @param listingId the listing identifier
     * @return the requested active listing
     */
    public StoreListing getStoreListing(UUID merchantId, UUID listingId) {
        return repository.findActiveByIdAndMerchantId(listingId, merchantId)
                .orElseThrow(() -> new ApiException(404, "Store listing not found."));
    }
}