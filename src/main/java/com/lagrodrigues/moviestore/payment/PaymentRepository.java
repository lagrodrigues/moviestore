package com.lagrodrigues.moviestore.payment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for payment persistence operations.
 */
@ApplicationScoped
public class PaymentRepository implements PanacheRepository<Payment> {

    @Inject
    EntityManager entityManager;

    /**
     * Returns a payment by identifier if it belongs to the given customer.
     *
     * @param paymentId  the payment identifier
     * @param customerId the customer identifier
     * @return the matching payment, if found
     */
    public Optional<Payment> findByIdAndCustomerId(UUID paymentId, UUID customerId) {
        return find("id = ?1 and customer.id = ?2", paymentId, customerId).firstResultOptional();
    }

    /**
     * Returns a payment by identifier if it belongs to the given merchant.
     *
     * @param paymentId  the payment identifier
     * @param merchantId the merchant identifier
     * @return the matching payment, if found
     */
    public Optional<Payment> findByIdAndMerchantId(UUID paymentId, UUID merchantId) {
        return find("id = ?1 and merchant.id = ?2", paymentId, merchantId).firstResultOptional();
    }

    /**
     * Returns a payment by idempotency key if it belongs to the given customer.
     *
     * @param customerId     the customer identifier
     * @param idempotencyKey the idempotency key
     * @return the matching payment, if found
     */
    public Optional<Payment> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey) {
        return find("customer.id = ?1 and idempotencyKey = ?2", customerId, idempotencyKey)
                .firstResultOptional();
    }

    /**
     * Returns a payment by identifier if it belongs to the given customer, applying a pessimistic write lock.
     *
     * @param paymentId  the payment identifier
     * @param customerId the customer identifier
     * @return the matching locked payment, if found
     */
    public Optional<Payment> findByIdAndCustomerIdForUpdate(UUID paymentId, UUID customerId) {
        return entityManager.createQuery(
                        "select p from Payment p where p.id = :paymentId and p.customer.id = :customerId",
                        Payment.class
                )
                .setParameter("paymentId", paymentId)
                .setParameter("customerId", customerId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst();
    }
}