package com.lagrodrigues.moviestore.payment;

import com.lagrodrigues.moviestore.common.ApiException;
import com.lagrodrigues.moviestore.listing.StoreListing;
import com.lagrodrigues.moviestore.listing.StoreListingRepository;
import com.lagrodrigues.moviestore.payment.dto.ConfirmPaymentRequest;
import com.lagrodrigues.moviestore.payment.dto.CreatePaymentRequest;
import com.lagrodrigues.moviestore.transaction.PaymentTransaction;
import com.lagrodrigues.moviestore.transaction.PaymentTransactionRepository;
import com.lagrodrigues.moviestore.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Provides application services related to payments.
 */
@ApplicationScoped
public class PaymentService {

    @Inject
    PaymentRepository paymentRepository;

    @Inject
    StoreListingRepository storeListingRepository;

    @Inject
    PaymentTransactionRepository paymentTransactionRepository;

    /**
     * Creates a new payment for the given customer and listing selection.
     *
     * <p>If the same idempotency key has already been used by the same customer,
     * the existing payment is returned instead of creating a duplicate.</p>
     *
     * <p>This method also handles the race condition where two concurrent requests
     * with the same idempotency key attempt to create the same payment at the same time.</p>
     *
     * @param customer       the authenticated customer
     * @param idempotencyKey the idempotency key supplied by the client
     * @param request        the payment creation request
     * @return the persisted or previously existing payment
     */
    @Transactional
    public Payment createPayment(User customer, String idempotencyKey, CreatePaymentRequest request) {
        validateIdempotencyKey(idempotencyKey);

        Payment existingPayment = paymentRepository
                .findByCustomerIdAndIdempotencyKey(customer.id, idempotencyKey)
                .orElse(null);

        if (existingPayment != null) {
            return existingPayment;
        }

        StoreListing listing = storeListingRepository.findActiveById(request.listingId())
                .orElseThrow(() -> new ApiException(404, "Store listing not found."));

        validateListingAvailability(listing);

        BigDecimal amount = resolveAmount(listing, request.saleType());

        Payment payment = new Payment();
        payment.customer = customer;
        payment.merchant = listing.merchant;
        payment.storeListing = listing;
        payment.idempotencyKey = idempotencyKey;
        payment.externalMovieId = listing.externalMovieId;
        payment.movieTitle = listing.movieTitle;
        payment.type = request.saleType();
        payment.amount = amount;
        payment.status = PaymentStatus.CREATED;
        payment.createdAt = OffsetDateTime.now();
        payment.updatedAt = OffsetDateTime.now();

        try {
            paymentRepository.persistAndFlush(payment);
            recordTransaction(payment, null, PaymentStatus.CREATED, "Payment created.");
            return payment;
        } catch (PersistenceException exception) {
            Payment paymentCreatedConcurrently = paymentRepository
                    .findByCustomerIdAndIdempotencyKey(customer.id, idempotencyKey)
                    .orElse(null);

            if (paymentCreatedConcurrently != null) {
                return paymentCreatedConcurrently;
            }

            throw exception;
        }
    }

    /**
     * Confirms an existing payment owned by the given customer.
     *
     * <p>The payment is only processed if it is currently in CREATED status.
     * Once confirmed successfully, the listing stock is decremented.</p>
     *
     * @param customer  the authenticated customer
     * @param paymentId the payment identifier
     * @param request   the confirmation request
     * @return the updated payment
     */
    @Transactional
    public Payment confirmPayment(User customer, UUID paymentId, ConfirmPaymentRequest request) {
        Payment payment = paymentRepository.findByIdAndCustomerIdForUpdate(paymentId, customer.id)
                .orElseThrow(() -> new ApiException(404, "Payment not found."));

        if (payment.status == PaymentStatus.SUCCEEDED) {
            throw new ApiException(409, "Payment has already been confirmed successfully.");
        }

        if (payment.status == PaymentStatus.FAILED) {
            String reason = payment.failureReason != null ? payment.failureReason : "UNKNOWN_REASON";
            throw new ApiException(409, "Payment confirmation has already failed previously. Reason: " + reason + ".");
        }

        if (payment.status == PaymentStatus.PROCESSING) {
            throw new ApiException(409, "Payment is already being processed.");
        }

        if (payment.status != PaymentStatus.CREATED) {
            throw new ApiException(409, "Payment cannot be confirmed from its current status.");
        }

        PaymentStatus previousStatus = payment.status;
        payment.status = PaymentStatus.PROCESSING;
        payment.updatedAt = OffsetDateTime.now();
        recordTransaction(payment, previousStatus, PaymentStatus.PROCESSING, "Payment confirmation is being processed.");

        StoreListing listing = storeListingRepository.findByIdForUpdate(payment.storeListing.id)
                .orElseThrow(() -> new ApiException(404, "Store listing not found."));

        if (listing.stock == null || listing.stock <= 0) {
            previousStatus = payment.status;
            payment.status = PaymentStatus.FAILED;
            payment.failureReason = "OUT_OF_STOCK";
            payment.updatedAt = OffsetDateTime.now();
            recordTransaction(payment, previousStatus, PaymentStatus.FAILED, "OUT_OF_STOCK");
            return payment;
        }

        if (!isCardValid(request)) {
            previousStatus = payment.status;
            payment.status = PaymentStatus.FAILED;
            payment.failureReason = "INVALID_CARD";
            payment.cardLast4 = extractLast4(request.cardNumber());
            payment.updatedAt = OffsetDateTime.now();
            recordTransaction(payment, previousStatus, PaymentStatus.FAILED, "INVALID_CARD");
            return payment;
        }

        listing.stock = listing.stock - 1;

        previousStatus = payment.status;
        payment.cardLast4 = extractLast4(request.cardNumber());
        payment.status = PaymentStatus.SUCCEEDED;
        payment.failureReason = null;
        payment.updatedAt = OffsetDateTime.now();
        recordTransaction(payment, previousStatus, PaymentStatus.SUCCEEDED, "Payment confirmed successfully.");

        return payment;
    }

    /**
     * Returns a payment owned by the given customer.
     *
     * @param customer  the authenticated customer
     * @param paymentId the payment identifier
     * @return the requested payment
     */
    public Payment getPayment(User customer, UUID paymentId) {
        return paymentRepository.findByIdAndCustomerId(paymentId, customer.id)
                .orElseThrow(() -> new ApiException(404, "Payment not found."));
    }

    /**
     * Returns a payment owned by the given merchant.
     *
     * @param merchant  the authenticated merchant
     * @param paymentId the payment identifier
     * @return the requested payment
     */
    public Payment getMerchantPayment(User merchant, UUID paymentId) {
        return paymentRepository.findByIdAndMerchantId(paymentId, merchant.id)
                .orElseThrow(() -> new ApiException(404, "Payment not found."));
    }

    /**
     * Validates that the listing is available for purchase or rental.
     *
     * @param listing the listing to validate
     */
    private void validateListingAvailability(StoreListing listing) {
        if (listing.stock == null || listing.stock <= 0) {
            throw new ApiException(400, "The selected listing is currently out of stock.");
        }
    }

    /**
     * Resolves the amount to be charged according to the selected sale type.
     *
     * @param listing  the selected store listing
     * @param saleType the selected sale type
     * @return the amount to be charged
     */
    private BigDecimal resolveAmount(StoreListing listing, SaleType saleType) {
        if (saleType == SaleType.PURCHASE) {
            if (listing.purchasePrice == null) {
                throw new ApiException(400, "This listing is not available for purchase.");
            }
            return listing.purchasePrice;
        }

        if (saleType == SaleType.RENT) {
            if (listing.rentalPrice == null) {
                throw new ApiException(400, "This listing is not available for rental.");
            }
            return listing.rentalPrice;
        }

        throw new ApiException(400, "Unsupported sale type.");
    }

    /**
     * Performs a simple validation of simulated card data.
     *
     * @param request the confirmation request
     * @return {@code true} if the card is considered valid; {@code false} otherwise
     */
    private boolean isCardValid(ConfirmPaymentRequest request) {
        String cardNumber = request.cardNumber();

        return cardNumber != null
                && cardNumber.matches("\\d{13,19}")
                && !"0000000000000000".equals(cardNumber)
                && request.expiryMonth() != null
                && request.expiryYear() != null
                && request.cvv() != null
                && request.cvv().matches("\\d{3,4}");
    }

    /**
     * Extracts the last four digits of the provided card number.
     *
     * @param cardNumber the full card number
     * @return the last four digits, or {@code null} if unavailable
     */
    private String extractLast4(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return null;
        }

        return cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * Records a lifecycle transaction entry for the given payment.
     *
     * @param payment    the payment
     * @param fromStatus the previous payment status
     * @param toStatus   the new payment status
     * @param message    the optional event message
     */
    private void recordTransaction(Payment payment, PaymentStatus fromStatus, PaymentStatus toStatus, String message) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.payment = payment;
        transaction.fromStatus = fromStatus;
        transaction.toStatus = toStatus;
        transaction.amount = payment.amount;
        transaction.message = message;
        transaction.createdAt = OffsetDateTime.now();

        paymentTransactionRepository.persist(transaction);
    }

    /**
     * Validates the provided idempotency key.
     *
     * @param idempotencyKey the idempotency key to validate
     */
    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(400, "Idempotency-Key header must be provided.");
        }

        if (idempotencyKey.length() > 100) {
            throw new ApiException(400, "Idempotency-Key must not exceed 100 characters.");
        }
    }
}