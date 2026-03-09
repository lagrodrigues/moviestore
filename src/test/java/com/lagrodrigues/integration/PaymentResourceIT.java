package com.lagrodrigues.integration;

import com.lagrodrigues.moviestore.listing.StoreListing;
import com.lagrodrigues.moviestore.user.User;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration tests for payment resource flows.
 */
@QuarkusTest
class PaymentResourceIT {

    private static final String CUSTOMER_API_KEY = "customer-key";

    private UUID listingId;

    @Inject
    jakarta.persistence.EntityManager entityManager;

    @BeforeEach
    @Transactional
    void setUp() {
        entityManager.createQuery("delete from PaymentTransaction").executeUpdate();
        entityManager.createQuery("delete from Payment").executeUpdate();
        entityManager.createQuery("delete from StoreListing").executeUpdate();

        User merchant = entityManager.find(User.class, UUID.fromString("11111111-1111-1111-1111-111111111111"));

        StoreListing listing = new StoreListing();
        listing.merchant = merchant;
        listing.externalMovieId = "tt0372784";
        listing.movieTitle = "Batman Begins";
        listing.movieYear = "2005";
        listing.moviePosterUrl = "https://poster.example/batman.jpg";
        listing.purchasePrice = new BigDecimal("9.99");
        listing.rentalPrice = new BigDecimal("3.99");
        listing.stock = 5;
        listing.active = true;
        listing.createdAt = OffsetDateTime.now();
        listing.updatedAt = OffsetDateTime.now();

        entityManager.persist(listing);

        listingId = listing.id;
    }

    @Test
    void shouldCreateConfirmAndRetrievePaymentSuccessfully() {
        String idempotencyKey = "it-payment-success-001";

        String paymentId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-API-KEY", CUSTOMER_API_KEY)
                        .header("Idempotency-Key", idempotencyKey)
                        .body(Map.of(
                                "listingId", listingId,
                                "saleType", "PURCHASE"
                        ))
                        .when()
                        .post("/api/payments")
                        .then()
                        .statusCode(200)
                        .body("id", notNullValue())
                        .body("listingId", equalTo(listingId.toString()))
                        .body("saleType", equalTo("PURCHASE"))
                        .body("amount", equalTo(9.99f))
                        .body("status", equalTo("CREATED"))
                        .extract()
                        .path("id");

        given()
                .contentType(ContentType.JSON)
                .header("X-API-KEY", CUSTOMER_API_KEY)
                .body(Map.of(
                        "cardNumber", "4111111111111111",
                        "cardHolderName", "John Doe",
                        "expiryMonth", 12,
                        "expiryYear", 2028,
                        "cvv", "123"
                ))
                .when()
                .post("/api/payments/" + paymentId + "/confirm")
                .then()
                .statusCode(200)
                .body("id", equalTo(paymentId))
                .body("status", equalTo("SUCCEEDED"))
                .body("failureReason", equalTo(null));

        given()
                .header("X-API-KEY", CUSTOMER_API_KEY)
                .when()
                .get("/api/payments/customer/" + paymentId)
                .then()
                .statusCode(200)
                .body("id", equalTo(paymentId))
                .body("status", equalTo("SUCCEEDED"))
                .body("movieTitle", equalTo("Batman Begins"))
                .body("saleType", equalTo("PURCHASE"))
                .body("amount", equalTo(9.99f));
    }

    @Test
    void shouldFailPaymentConfirmationWithInvalidCard() {
        String idempotencyKey = "it-payment-failed-001";

        String paymentId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-API-KEY", CUSTOMER_API_KEY)
                        .header("Idempotency-Key", idempotencyKey)
                        .body(Map.of(
                                "listingId", listingId,
                                "saleType", "RENT"
                        ))
                        .when()
                        .post("/api/payments")
                        .then()
                        .statusCode(200)
                        .body("status", equalTo("CREATED"))
                        .extract()
                        .path("id");

        given()
                .contentType(ContentType.JSON)
                .header("X-API-KEY", CUSTOMER_API_KEY)
                .body(Map.of(
                        "cardNumber", "0000000000000000",
                        "cardHolderName", "John Doe",
                        "expiryMonth", 12,
                        "expiryYear", 2028,
                        "cvv", "123"
                ))
                .when()
                .post("/api/payments/" + paymentId + "/confirm")
                .then()
                .statusCode(200)
                .body("id", equalTo(paymentId))
                .body("status", equalTo("FAILED"))
                .body("failureReason", equalTo("INVALID_CARD"));

        given()
                .header("X-API-KEY", CUSTOMER_API_KEY)
                .when()
                .get("/api/payments/customer/" + paymentId)
                .then()
                .statusCode(200)
                .body("id", equalTo(paymentId))
                .body("status", equalTo("FAILED"))
                .body("failureReason", equalTo("INVALID_CARD"))
                .body("saleType", equalTo("RENT"))
                .body("amount", equalTo(3.99f));
    }

    @Test
    void shouldReturnSamePaymentForSameIdempotencyKey() {
        String idempotencyKey = "it-payment-idempotent-001";

        String firstPaymentId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-API-KEY", CUSTOMER_API_KEY)
                        .header("Idempotency-Key", idempotencyKey)
                        .body(Map.of(
                                "listingId", listingId,
                                "saleType", "PURCHASE"
                        ))
                        .when()
                        .post("/api/payments")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("id");

        String secondPaymentId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-API-KEY", CUSTOMER_API_KEY)
                        .header("Idempotency-Key", idempotencyKey)
                        .body(Map.of(
                                "listingId", listingId,
                                "saleType", "PURCHASE"
                        ))
                        .when()
                        .post("/api/payments")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("id");

        org.junit.jupiter.api.Assertions.assertEquals(firstPaymentId, secondPaymentId);
    }
}