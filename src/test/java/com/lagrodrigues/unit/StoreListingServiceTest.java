package com.lagrodrigues.unit;

import com.lagrodrigues.moviestore.common.ApiException;
import com.lagrodrigues.moviestore.listing.StoreListing;
import com.lagrodrigues.moviestore.listing.StoreListingRepository;
import com.lagrodrigues.moviestore.listing.StoreListingService;
import com.lagrodrigues.moviestore.listing.dto.CreateListingRequest;
import com.lagrodrigues.moviestore.listing.dto.UpdateListingRequest;
import com.lagrodrigues.moviestore.movie.MovieService;
import com.lagrodrigues.moviestore.movie.dto.MovieDetailsResult;
import com.lagrodrigues.moviestore.user.User;
import com.lagrodrigues.moviestore.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StoreListingService}.
 */
@ExtendWith(MockitoExtension.class)
class StoreListingServiceTest {

    @Mock
    StoreListingRepository repository;

    @Mock
    MovieService movieService;

    @InjectMocks
    StoreListingService service;

    @Test
    void shouldCreateListingSuccessfullyWithPurchasePriceOnly() {
        User merchant = merchant();
        CreateListingRequest request = new CreateListingRequest(
                "tt0372784",
                new BigDecimal("9.99"),
                null,
                10
        );

        when(movieService.getMovieById("tt0372784"))
                .thenReturn(movieDetails());

        StoreListing result = service.createListing(merchant, request);

        assertNotNull(result);
        assertEquals(merchant, result.merchant);
        assertEquals("tt0372784", result.externalMovieId);
        assertEquals("Batman Begins", result.movieTitle);
        assertEquals("2005", result.movieYear);
        assertEquals(new BigDecimal("9.99"), result.purchasePrice);
        assertNull(result.rentalPrice);
        assertEquals(10, result.stock);
        assertTrue(result.active);

        verify(repository).persist(any(StoreListing.class));
    }

    @Test
    void shouldCreateListingSuccessfullyWithRentalPriceOnly() {
        User merchant = merchant();
        CreateListingRequest request = new CreateListingRequest(
                "tt0372784",
                null,
                new BigDecimal("3.99"),
                8
        );

        when(movieService.getMovieById("tt0372784"))
                .thenReturn(movieDetails());

        StoreListing result = service.createListing(merchant, request);

        assertNotNull(result);
        assertNull(result.purchasePrice);
        assertEquals(new BigDecimal("3.99"), result.rentalPrice);
        assertEquals(8, result.stock);

        verify(repository).persist(any(StoreListing.class));
    }

    @Test
    void shouldFailCreateListingWhenBothPricesAreMissing() {
        User merchant = merchant();
        CreateListingRequest request = new CreateListingRequest(
                "tt0372784",
                null,
                null,
                10
        );

        ApiException exception = assertThrows(ApiException.class, () ->
                service.createListing(merchant, request)
        );

        assertEquals(400, exception.getStatusCode());
        assertEquals("At least one of purchasePrice or rentalPrice must be provided.", exception.getMessage());
    }

    @Test
    void shouldFailCreateListingWhenStockIsMissing() {
        User merchant = merchant();
        CreateListingRequest request = new CreateListingRequest(
                "tt0372784",
                new BigDecimal("9.99"),
                null,
                null
        );

        ApiException exception = assertThrows(ApiException.class, () ->
                service.createListing(merchant, request)
        );

        assertEquals(400, exception.getStatusCode());
        assertEquals("Stock must be provided.", exception.getMessage());
    }

    @Test
    void shouldFailCreateListingWhenStockIsNegative() {
        User merchant = merchant();
        CreateListingRequest request = new CreateListingRequest(
                "tt0372784",
                new BigDecimal("9.99"),
                null,
                -1
        );

        ApiException exception = assertThrows(ApiException.class, () ->
                service.createListing(merchant, request)
        );

        assertEquals(400, exception.getStatusCode());
        assertEquals("Stock must be greater than or equal to zero.", exception.getMessage());
    }

    @Test
    void shouldReturnMerchantListings() {
        User merchant = merchant();
        StoreListing listing = listing(merchant);

        when(repository.findByMerchantId(merchant.id))
                .thenReturn(List.of(listing));

        List<StoreListing> result = service.getMerchantListings(merchant);

        assertEquals(1, result.size());
        assertEquals(listing.id, result.getFirst().id);
    }

    @Test
    void shouldReturnMerchantListingById() {
        User merchant = merchant();
        StoreListing listing = listing(merchant);

        when(repository.findByIdAndMerchantId(listing.id, merchant.id))
                .thenReturn(Optional.of(listing));

        StoreListing result = service.getMerchantListingById(merchant, listing.id);

        assertEquals(listing.id, result.id);
    }

    @Test
    void shouldFailWhenMerchantListingIsNotFound() {
        User merchant = merchant();
        UUID listingId = UUID.randomUUID();

        when(repository.findByIdAndMerchantId(listingId, merchant.id))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () ->
                service.getMerchantListingById(merchant, listingId)
        );

        assertEquals(404, exception.getStatusCode());
        assertEquals("Listing not found.", exception.getMessage());
    }

    @Test
    void shouldUpdateListingSuccessfully() {
        User merchant = merchant();
        StoreListing listing = listing(merchant);
        listing.purchasePrice = new BigDecimal("9.99");
        listing.rentalPrice = new BigDecimal("3.99");
        listing.stock = 10;
        listing.active = true;

        UpdateListingRequest request = new UpdateListingRequest(
                new BigDecimal("12.99"),
                new BigDecimal("4.99"),
                7,
                false
        );

        when(repository.findByIdAndMerchantId(listing.id, merchant.id))
                .thenReturn(Optional.of(listing));

        StoreListing result = service.updateListing(merchant, listing.id, request);

        assertEquals(new BigDecimal("12.99"), result.purchasePrice);
        assertEquals(new BigDecimal("4.99"), result.rentalPrice);
        assertEquals(7, result.stock);
        assertFalse(result.active);
        assertNotNull(result.updatedAt);
    }

    @Test
    void shouldFailUpdateWhenBothPricesBecomeNull() {
        User merchant = merchant();
        StoreListing listing = listing(merchant);
        listing.purchasePrice = null;
        listing.rentalPrice = null;
        listing.stock = 10;

        UpdateListingRequest request = new UpdateListingRequest(
                null,
                null,
                5,
                true
        );

        when(repository.findByIdAndMerchantId(listing.id, merchant.id))
                .thenReturn(Optional.of(listing));

        ApiException exception = assertThrows(ApiException.class, () ->
                service.updateListing(merchant, listing.id, request)
        );

        assertEquals(400, exception.getStatusCode());
        assertEquals("At least one of purchasePrice or rentalPrice must be configured.", exception.getMessage());
    }

    @Test
    void shouldFailUpdateWhenStockBecomesNegative() {
        User merchant = merchant();
        StoreListing listing = listing(merchant);
        listing.purchasePrice = new BigDecimal("9.99");
        listing.rentalPrice = null;
        listing.stock = -1;

        UpdateListingRequest request = new UpdateListingRequest(
                null,
                null,
                null,
                true
        );

        when(repository.findByIdAndMerchantId(listing.id, merchant.id))
                .thenReturn(Optional.of(listing));

        ApiException exception = assertThrows(ApiException.class, () ->
                service.updateListing(merchant, listing.id, request)
        );

        assertEquals(400, exception.getStatusCode());
        assertEquals("Stock must be greater than or equal to zero.", exception.getMessage());
    }

    private User merchant() {
        User user = new User();
        user.id = UUID.randomUUID();
        user.username = "merchant";
        user.role = UserRole.MERCHANT;
        user.createdAt = OffsetDateTime.now();
        return user;
    }

    private MovieDetailsResult movieDetails() {
        return new MovieDetailsResult(
                "tt0372784",
                "Batman Begins",
                "2005",
                "https://poster.example/batman.jpg",
                "Christian Bale, Michael Caine",
                "A young Bruce Wayne becomes Batman."
        );
    }

    private StoreListing listing(User merchant) {
        StoreListing listing = new StoreListing();
        listing.id = UUID.randomUUID();
        listing.merchant = merchant;
        listing.externalMovieId = "tt0372784";
        listing.movieTitle = "Batman Begins";
        listing.movieYear = "2005";
        listing.moviePosterUrl = "https://poster.example/batman.jpg";
        listing.createdAt = OffsetDateTime.now();
        listing.updatedAt = OffsetDateTime.now();
        return listing;
    }
}