package com.lagrodrigues.integration;

import com.lagrodrigues.moviestore.listing.StoreListing;
import com.lagrodrigues.moviestore.payment.Payment;
import com.lagrodrigues.moviestore.payment.PaymentStatus;
import com.lagrodrigues.moviestore.payment.SaleType;
import com.lagrodrigues.moviestore.transaction.PaymentTransaction;
import com.lagrodrigues.moviestore.user.User;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * Integration tests for transaction listing endpoints.
 */
@QuarkusTest
class TransactionResourceIT {

    private static final String MERCHANT_API_KEY = "admin-key";
    private static final String CUSTOMER_API_KEY = "customer-key";

    @Inject
    EntityManager entityManager;

    private UUID merchantId;
    private UUID customerId;
    private UUID paymentId;

    @BeforeEach
    @Transactional
    void setUp() {
        entityManager.createQuery("delete from PaymentTransaction").executeUpdate();
        entityManager.createQuery("delete from Payment").executeUpdate();
        entityManager.createQuery("delete from StoreListing").executeUpdate();

        User merchant = entityManager.find(User.class, UUID.fromString("11111111-1111-1111-1111-111111111111"));
        User customer = entityManager.find(User.class, UUID.fromString("22222222-2222-2222-2222-222222222222"));

        merchantId = merchant.id;
        customerId = customer.id;

        StoreListing listing = new StoreListing();
        listing.merchant = merchant;
        listing.externalMovieId = "tt0372784";
        listing.movieTitle = "Batman Begins";
        listing.movieYear = "2005";
        listing.moviePosterUrl = "https://poster.example/batman.jpg";
        listing.purchasePrice = new BigDecimal("9.99");
        listing.rentalPrice = new BigDecimal("3.99");
        listing.stock = 4;
        listing.active = true;
        listing.createdAt = OffsetDateTime.now();
        listing.updatedAt = OffsetDateTime.now();

        entityManager.persist(listing);
        entityManager.flush();

        Payment payment = new Payment();
        payment.customer = customer;
        payment.merchant = merchant;
        payment.storeListing = listing;
        payment.idempotencyKey = "txn-it-001";
        payment.externalMovieId = "tt0372784";
        payment.movieTitle = "Batman Begins";
        payment.type = SaleType.PURCHASE;
        payment.amount = new BigDecimal("9.99");
        payment.status = PaymentStatus.SUCCEEDED;
        payment.createdAt = OffsetDateTime.now();
        payment.updatedAt = OffsetDateTime.now();

        entityManager.persist(payment);
        entityManager.flush();

        paymentId = payment.id;

        PaymentTransaction tx1 = new PaymentTransaction();
        tx1.payment = payment;
        tx1.fromStatus = null;
        tx1.toStatus = PaymentStatus.CREATED;
        tx1.amount = new BigDecimal("9.99");
        tx1.message = "Payment created.";
        tx1.createdAt = OffsetDateTime.now().minusSeconds(3);
        entityManager.persist(tx1);

        PaymentTransaction tx2 = new PaymentTransaction();
        tx2.payment = payment;
        tx2.fromStatus = PaymentStatus.CREATED;
        tx2.toStatus = PaymentStatus.PROCESSING;
        tx2.amount = new BigDecimal("9.99");
        tx2.message = "Payment confirmation is being processed.";
        tx2.createdAt = OffsetDateTime.now().minusSeconds(2);
        entityManager.persist(tx2);

        PaymentTransaction tx3 = new PaymentTransaction();
        tx3.payment = payment;
        tx3.fromStatus = PaymentStatus.PROCESSING;
        tx3.toStatus = PaymentStatus.SUCCEEDED;
        tx3.amount = new BigDecimal("9.99");
        tx3.message = "Payment confirmed successfully.";
        tx3.createdAt = OffsetDateTime.now().minusSeconds(1);
        entityManager.persist(tx3);

        entityManager.flush();
    }

    @Test
    void shouldReturnCustomerTransactions() {
        given()
                .header("X-API-KEY", CUSTOMER_API_KEY)
                .accept(ContentType.JSON)
                .when()
                .get("/api/transactions/customer")
                .then()
                .statusCode(200)
                .body("items", hasSize(3))
                .body("totalItems", equalTo(3))
                .body("totalPages", equalTo(1))
                .body("items[0].paymentId", equalTo(paymentId.toString()));
    }

    @Test
    void shouldFilterCustomerTransactionsByPaymentId() {
        given()
                .header("X-API-KEY", CUSTOMER_API_KEY)
                .queryParam("paymentId", paymentId)
                .accept(ContentType.JSON)
                .when()
                .get("/api/transactions/customer")
                .then()
                .statusCode(200)
                .body("items", hasSize(3))
                .body("totalItems", equalTo(3));
    }

    @Test
    void shouldFilterCustomerTransactionsByMerchantId() {
        given()
                .header("X-API-KEY", CUSTOMER_API_KEY)
                .queryParam("merchantId", merchantId)
                .accept(ContentType.JSON)
                .when()
                .get("/api/transactions/customer")
                .then()
                .statusCode(200)
                .body("items", hasSize(3))
                .body("totalItems", equalTo(3));
    }

    @Test
    void shouldFilterCustomerTransactionsByToStatus() {
        given()
                .header("X-API-KEY", CUSTOMER_API_KEY)
                .queryParam("toStatus", "SUCCEEDED")
                .accept(ContentType.JSON)
                .when()
                .get("/api/transactions/customer")
                .then()
                .statusCode(200)
                .body("items", hasSize(1))
                .body("totalItems", equalTo(1))
                .body("items[0].fromStatus", equalTo("PROCESSING"))
                .body("items[0].toStatus", equalTo("SUCCEEDED"));
    }

    @Test
    void shouldReturnMerchantTransactions() {
        given()
                .header("X-API-KEY", MERCHANT_API_KEY)
                .accept(ContentType.JSON)
                .when()
                .get("/api/transactions/merchant")
                .then()
                .statusCode(200)
                .body("items", hasSize(3))
                .body("totalItems", equalTo(3))
                .body("items[0].paymentId", equalTo(paymentId.toString()));
    }

    @Test
    void shouldFilterMerchantTransactionsByCustomerId() {
        given()
                .header("X-API-KEY", MERCHANT_API_KEY)
                .queryParam("customerId", customerId)
                .accept(ContentType.JSON)
                .when()
                .get("/api/transactions/merchant")
                .then()
                .statusCode(200)
                .body("items", hasSize(3))
                .body("totalItems", equalTo(3));
    }

    @Test
    void shouldFilterMerchantTransactionsByPaymentId() {
        given()
                .header("X-API-KEY", MERCHANT_API_KEY)
                .queryParam("paymentId", paymentId)
                .accept(ContentType.JSON)
                .when()
                .get("/api/transactions/merchant")
                .then()
                .statusCode(200)
                .body("items", hasSize(3))
                .body("totalItems", equalTo(3));
    }

    @Test
    void shouldPaginateCustomerTransactions() {
        given()
                .header("X-API-KEY", CUSTOMER_API_KEY)
                .queryParam("page", 0)
                .queryParam("size", 2)
                .accept(ContentType.JSON)
                .when()
                .get("/api/transactions/customer")
                .then()
                .statusCode(200)
                .body("items", hasSize(2))
                .body("page", equalTo(0))
                .body("size", equalTo(2))
                .body("totalItems", equalTo(3))
                .body("totalPages", equalTo(2));
    }
}