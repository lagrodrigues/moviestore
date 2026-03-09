package com.lagrodrigues.unit;

import com.lagrodrigues.moviestore.common.ApiException;
import com.lagrodrigues.moviestore.listing.StoreListing;
import com.lagrodrigues.moviestore.listing.StoreListingRepository;
import com.lagrodrigues.moviestore.payment.*;
import com.lagrodrigues.moviestore.payment.dto.ConfirmPaymentRequest;
import com.lagrodrigues.moviestore.payment.dto.CreatePaymentRequest;
import com.lagrodrigues.moviestore.transaction.PaymentTransaction;
import com.lagrodrigues.moviestore.transaction.PaymentTransactionRepository;
import com.lagrodrigues.moviestore.user.User;
import com.lagrodrigues.moviestore.user.UserRole;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PaymentService}.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    StoreListingRepository storeListingRepository;

    @Mock
    PaymentTransactionRepository paymentTransactionRepository;

    @InjectMocks
    PaymentService paymentService;

    @Test
    void shouldCreatePurchasePaymentSuccessfully() {
        User customer = customer();
        StoreListing listing = listing();
        listing.purchasePrice = new BigDecimal("9.99");
        listing.rentalPrice = new BigDecimal("3.99");
        listing.stock = 10;

        CreatePaymentRequest request = new CreatePaymentRequest(listing.id, SaleType.PURCHASE);

        when(paymentRepository.findByCustomerIdAndIdempotencyKey(customer.id, "idem-1"))
                .thenReturn(Optional.empty());
        when(storeListingRepository.findActiveById(listing.id))
                .thenReturn(Optional.of(listing));

        Payment result = paymentService.createPayment(customer, "idem-1", request);

        assertNotNull(result);
        assertEquals(customer, result.customer);
        assertEquals(listing.merchant, result.merchant);
        assertEquals(listing, result.storeListing);
        assertEquals(SaleType.PURCHASE, result.type);
        assertEquals(new BigDecimal("9.99"), result.amount);
        assertEquals(PaymentStatus.CREATED, result.status);
        assertEquals("idem-1", result.idempotencyKey);

        verify(paymentRepository).persistAndFlush(any(Payment.class));
        verify(paymentTransactionRepository).persist(any(PaymentTransaction.class));
    }

    @Test
    void shouldReturnExistingPaymentWhenIdempotencyKeyAlreadyExists() {
        User customer = customer();
        Payment existing = payment(customer, PaymentStatus.CREATED);

        when(paymentRepository.findByCustomerIdAndIdempotencyKey(customer.id, "idem-1"))
                .thenReturn(Optional.of(existing));

        Payment result = paymentService.createPayment(
                customer,
                "idem-1",
                new CreatePaymentRequest(UUID.randomUUID(), SaleType.PURCHASE)
        );

        assertSame(existing, result);
        verify(paymentRepository, never()).persistAndFlush(any());
        verify(paymentTransactionRepository, never()).persist(any(PaymentTransaction.class));
    }

    @Test
    void shouldReturnExistingPaymentWhenPersistenceExceptionOccursDuringIdempotentCreate() {
        User customer = customer();
        StoreListing listing = listing();
        listing.purchasePrice = new BigDecimal("9.99");
        listing.stock = 10;

        Payment concurrentlyCreated = payment(customer, PaymentStatus.CREATED);

        when(paymentRepository.findByCustomerIdAndIdempotencyKey(customer.id, "idem-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(concurrentlyCreated));

        when(storeListingRepository.findActiveById(listing.id))
                .thenReturn(Optional.of(listing));

        doThrow(new PersistenceException("duplicate key"))
                .when(paymentRepository).persistAndFlush(any(Payment.class));

        Payment result = paymentService.createPayment(
                customer,
                "idem-1",
                new CreatePaymentRequest(listing.id, SaleType.PURCHASE)
        );

        assertSame(concurrentlyCreated, result);
    }

    @Test
    void shouldFailWhenListingIsOutOfStockDuringCreate() {
        User customer = customer();
        StoreListing listing = listing();
        listing.purchasePrice = new BigDecimal("9.99");
        listing.stock = 0;

        when(paymentRepository.findByCustomerIdAndIdempotencyKey(customer.id, "idem-1"))
                .thenReturn(Optional.empty());
        when(storeListingRepository.findActiveById(listing.id))
                .thenReturn(Optional.of(listing));

        ApiException exception = assertThrows(ApiException.class, () ->
                paymentService.createPayment(
                        customer,
                        "idem-1",
                        new CreatePaymentRequest(listing.id, SaleType.PURCHASE)
                )
        );

        assertEquals(400, exception.getStatusCode());
        assertEquals("The selected listing is currently out of stock.", exception.getMessage());
    }

    @Test
    void shouldFailWhenPurchaseIsNotAvailable() {
        User customer = customer();
        StoreListing listing = listing();
        listing.purchasePrice = null;
        listing.rentalPrice = new BigDecimal("3.99");
        listing.stock = 10;

        when(paymentRepository.findByCustomerIdAndIdempotencyKey(customer.id, "idem-1"))
                .thenReturn(Optional.empty());
        when(storeListingRepository.findActiveById(listing.id))
                .thenReturn(Optional.of(listing));

        ApiException exception = assertThrows(ApiException.class, () ->
                paymentService.createPayment(
                        customer,
                        "idem-1",
                        new CreatePaymentRequest(listing.id, SaleType.PURCHASE)
                )
        );

        assertEquals(400, exception.getStatusCode());
        assertEquals("This listing is not available for purchase.", exception.getMessage());
    }

    @Test
    void shouldConfirmPaymentSuccessfully() {
        User customer = customer();
        StoreListing listing = listing();
        listing.stock = 5;

        Payment payment = payment(customer, PaymentStatus.CREATED);
        payment.storeListing = listing;
        payment.amount = new BigDecimal("9.99");

        ConfirmPaymentRequest request = new ConfirmPaymentRequest(
                "4111111111111111",
                "John Doe",
                12,
                2028,
                "123"
        );

        when(paymentRepository.findByIdAndCustomerIdForUpdate(payment.id, customer.id))
                .thenReturn(Optional.of(payment));
        when(storeListingRepository.findByIdForUpdate(listing.id))
                .thenReturn(Optional.of(listing));

        Payment result = paymentService.confirmPayment(customer, payment.id, request);

        assertEquals(PaymentStatus.SUCCEEDED, result.status);
        assertEquals("1111", result.cardLast4);
        assertNull(result.failureReason);
        assertEquals(4, listing.stock);

        verify(paymentTransactionRepository, times(2)).persist(any(PaymentTransaction.class));
    }

    @Test
    void shouldFailConfirmationWhenCardIsInvalid() {
        User customer = customer();
        StoreListing listing = listing();
        listing.stock = 5;

        Payment payment = payment(customer, PaymentStatus.CREATED);
        payment.storeListing = listing;
        payment.amount = new BigDecimal("9.99");

        ConfirmPaymentRequest request = new ConfirmPaymentRequest(
                "0000000000000000",
                "John Doe",
                12,
                2028,
                "123"
        );

        when(paymentRepository.findByIdAndCustomerIdForUpdate(payment.id, customer.id))
                .thenReturn(Optional.of(payment));
        when(storeListingRepository.findByIdForUpdate(listing.id))
                .thenReturn(Optional.of(listing));

        Payment result = paymentService.confirmPayment(customer, payment.id, request);

        assertEquals(PaymentStatus.FAILED, result.status);
        assertEquals("INVALID_CARD", result.failureReason);
        assertEquals(5, listing.stock);
    }

    @Test
    void shouldFailConfirmationWhenPaymentAlreadySucceeded() {
        User customer = customer();
        Payment payment = payment(customer, PaymentStatus.SUCCEEDED);

        when(paymentRepository.findByIdAndCustomerIdForUpdate(payment.id, customer.id))
                .thenReturn(Optional.of(payment));

        ApiException exception = assertThrows(ApiException.class, () ->
                paymentService.confirmPayment(
                        customer,
                        payment.id,
                        new ConfirmPaymentRequest("4111111111111111", "John Doe", 12, 2028, "123")
                )
        );

        assertEquals(409, exception.getStatusCode());
        assertEquals("Payment has already been confirmed successfully.", exception.getMessage());
    }

    @Test
    void shouldFailWhenIdempotencyKeyIsMissing() {
        User customer = customer();

        ApiException exception = assertThrows(ApiException.class, () ->
                paymentService.createPayment(
                        customer,
                        "   ",
                        new CreatePaymentRequest(UUID.randomUUID(), SaleType.PURCHASE)
                )
        );

        assertEquals(400, exception.getStatusCode());
        assertEquals("Idempotency-Key header must be provided.", exception.getMessage());
    }

    private User customer() {
        User user = new User();
        user.id = UUID.randomUUID();
        user.username = "customer";
        user.role = UserRole.CUSTOMER;
        user.createdAt = OffsetDateTime.now();
        return user;
    }

    private User merchant() {
        User user = new User();
        user.id = UUID.randomUUID();
        user.username = "merchant";
        user.role = UserRole.MERCHANT;
        user.createdAt = OffsetDateTime.now();
        return user;
    }

    private StoreListing listing() {
        StoreListing listing = new StoreListing();
        listing.id = UUID.randomUUID();
        listing.externalMovieId = "tt0372784";
        listing.movieTitle = "Batman Begins";
        listing.movieYear = "2005";
        listing.active = true;
        listing.merchant = merchant();
        return listing;
    }

    private Payment payment(User customer, PaymentStatus status) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.customer = customer;
        payment.merchant = merchant();
        payment.status = status;
        payment.createdAt = OffsetDateTime.now();
        payment.updatedAt = OffsetDateTime.now();
        payment.externalMovieId = "tt0372784";
        payment.movieTitle = "Batman Begins";
        payment.type = SaleType.PURCHASE;
        payment.amount = new BigDecimal("9.99");
        return payment;
    }
}