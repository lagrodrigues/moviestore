package com.lagrodrigues.integration;

import com.lagrodrigues.moviestore.movie.MovieService;
import com.lagrodrigues.moviestore.movie.dto.MovieDetailsResult;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * Integration tests for store listing resource flows.
 */
@QuarkusTest
class StoreListingResourceIT {

    private static final String MERCHANT_API_KEY = "admin-key";
    private static final String CUSTOMER_API_KEY = "customer-key";
    private static final UUID MERCHANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Inject
    EntityManager entityManager;

    @InjectMock
    MovieService movieService;

    @BeforeEach
    @Transactional
    void setUp() {
        entityManager.createQuery("delete from PaymentTransaction").executeUpdate();
        entityManager.createQuery("delete from Payment").executeUpdate();
        entityManager.createQuery("delete from StoreListing").executeUpdate();

        when(movieService.getMovieById("tt0372784")).thenReturn(
                new MovieDetailsResult(
                        "tt0372784",
                        "Batman Begins",
                        "2005",
                        "https://poster.example/batman.jpg",
                        "https://imdb.com/title/tt0372784",
                        "Christian Bale, Michael Caine"
                )
        );
    }

    @Test
    void shouldCreateMerchantListingSuccessfully() {
        given()
                .contentType(ContentType.JSON)
                .header("X-API-KEY", MERCHANT_API_KEY)
                .body(Map.of(
                        "externalMovieId", "tt0372784",
                        "purchasePrice", 9.99,
                        "rentalPrice", 3.99,
                        "stock", 10
                ))
                .when()
                .post("/api/merchant/listings")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("externalMovieId", equalTo("tt0372784"))
                .body("title", equalTo("Batman Begins"))
                .body("year", equalTo("2005"))
                .body("purchasePrice", equalTo(9.99f))
                .body("rentalPrice", equalTo(3.99f))
                .body("stock", equalTo(10))
                .body("active", equalTo(true));
    }

    @Test
    void shouldReturnMerchantListings() {
        String listingId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-API-KEY", MERCHANT_API_KEY)
                        .body(Map.of(
                                "externalMovieId", "tt0372784",
                                "purchasePrice", 9.99,
                                "rentalPrice", 3.99,
                                "stock", 10
                        ))
                        .when()
                        .post("/api/merchant/listings")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("id");

        given()
                .header("X-API-KEY", MERCHANT_API_KEY)
                .when()
                .get("/api/merchant/listings")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].id", equalTo(listingId))
                .body("[0].title", equalTo("Batman Begins"));
    }

    @Test
    void shouldReturnMerchantListingById() {
        String listingId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-API-KEY", MERCHANT_API_KEY)
                        .body(Map.of(
                                "externalMovieId", "tt0372784",
                                "purchasePrice", 9.99,
                                "rentalPrice", 3.99,
                                "stock", 10
                        ))
                        .when()
                        .post("/api/merchant/listings")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("id");

        given()
                .header("X-API-KEY", MERCHANT_API_KEY)
                .when()
                .get("/api/merchant/listings/" + listingId)
                .then()
                .statusCode(200)
                .body("id", equalTo(listingId))
                .body("title", equalTo("Batman Begins"))
                .body("stock", equalTo(10));
    }

    @Test
    void shouldUpdateMerchantListingSuccessfully() {
        String listingId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-API-KEY", MERCHANT_API_KEY)
                        .body(Map.of(
                                "externalMovieId", "tt0372784",
                                "purchasePrice", 9.99,
                                "rentalPrice", 3.99,
                                "stock", 10
                        ))
                        .when()
                        .post("/api/merchant/listings")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("id");

        given()
                .contentType(ContentType.JSON)
                .header("X-API-KEY", MERCHANT_API_KEY)
                .body(Map.of(
                        "purchasePrice", 12.99,
                        "rentalPrice", 4.99,
                        "stock", 7,
                        "active", false
                ))
                .when()
                .put("/api/merchant/listings/" + listingId)
                .then()
                .statusCode(200)
                .body("id", equalTo(listingId))
                .body("purchasePrice", equalTo(12.99f))
                .body("rentalPrice", equalTo(4.99f))
                .body("stock", equalTo(7))
                .body("active", equalTo(false));
    }

    @Test
    void shouldBrowseMerchantStoreAsCustomer() {
        String listingId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-API-KEY", MERCHANT_API_KEY)
                        .body(Map.of(
                                "externalMovieId", "tt0372784",
                                "purchasePrice", 9.99,
                                "rentalPrice", 3.99,
                                "stock", 10
                        ))
                        .when()
                        .post("/api/merchant/listings")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("id");

        given()
                .header("X-API-KEY", CUSTOMER_API_KEY)
                .when()
                .get("/api/stores/" + MERCHANT_ID + "/listings")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].id", equalTo(listingId))
                .body("[0].title", equalTo("Batman Begins"))
                .body("[0].purchasePrice", equalTo(9.99f))
                .body("[0].rentalPrice", equalTo(3.99f))
                .body("[0].stock", equalTo(10));
    }

    @Test
    void shouldReturnStoreListingDetailsAsCustomer() {
        String listingId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-API-KEY", MERCHANT_API_KEY)
                        .body(Map.of(
                                "externalMovieId", "tt0372784",
                                "purchasePrice", 9.99,
                                "rentalPrice", 3.99,
                                "stock", 10
                        ))
                        .when()
                        .post("/api/merchant/listings")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("id");

        given()
                .header("X-API-KEY", CUSTOMER_API_KEY)
                .when()
                .get("/api/stores/" + MERCHANT_ID + "/listings/" + listingId)
                .then()
                .statusCode(200)
                .body("id", equalTo(listingId))
                .body("title", equalTo("Batman Begins"))
                .body("purchasePrice", equalTo(9.99f))
                .body("rentalPrice", equalTo(3.99f))
                .body("stock", equalTo(10))
                .body("available", equalTo(true));
    }
}