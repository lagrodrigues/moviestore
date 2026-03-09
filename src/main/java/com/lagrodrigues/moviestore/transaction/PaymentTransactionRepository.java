package com.lagrodrigues.moviestore.transaction;

import com.lagrodrigues.moviestore.payment.PaymentStatus;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Repository for payment transaction history persistence operations.
 */
@ApplicationScoped
public class PaymentTransactionRepository implements PanacheRepository<PaymentTransaction> {

    /**
     * Builds the merchant transaction history query according to the provided filters and sorting options.
     *
     * @param merchantId    the merchant identifier
     * @param paymentId     the optional payment identifier filter
     * @param customerId    the optional customer identifier filter
     * @param fromStatus    the optional origin status filter
     * @param toStatus      the optional destination status filter
     * @param sortBy        the requested sort field
     * @param sortDirection the requested sort direction
     * @return the configured query
     */
    public PanacheQuery<PaymentTransaction> findMerchantTransactions(
            UUID merchantId,
            UUID paymentId,
            UUID customerId,
            PaymentStatus fromStatus,
            PaymentStatus toStatus,
            String sortBy,
            String sortDirection
    ) {
        StringBuilder query = new StringBuilder("payment.merchant.id = ?1");
        java.util.List<Object> parameters = new java.util.ArrayList<>();
        parameters.add(merchantId);

        if (paymentId != null) {
            query.append(" and payment.id = ?").append(parameters.size() + 1);
            parameters.add(paymentId);
        }

        if (customerId != null) {
            query.append(" and payment.customer.id = ?").append(parameters.size() + 1);
            parameters.add(customerId);
        }

        if (fromStatus != null) {
            query.append(" and fromStatus = ?").append(parameters.size() + 1);
            parameters.add(fromStatus);
        }

        if (toStatus != null) {
            query.append(" and toStatus = ?").append(parameters.size() + 1);
            parameters.add(toStatus);
        }

        query.append(" order by ")
                .append(resolveSortField(sortBy))
                .append(" ")
                .append(resolveSortDirection(sortDirection));

        return find(query.toString(), parameters.toArray());
    }

    /**
     * Builds the customer transaction history query according to the provided filters and sorting options.
     *
     * @param customerId    the customer identifier
     * @param paymentId     the optional payment identifier filter
     * @param merchantId    the optional merchant identifier filter
     * @param fromStatus    the optional origin status filter
     * @param toStatus      the optional destination status filter
     * @param sortBy        the requested sort field
     * @param sortDirection the requested sort direction
     * @return the configured query
     */
    public PanacheQuery<PaymentTransaction> findCustomerTransactions(
            UUID customerId,
            UUID paymentId,
            UUID merchantId,
            PaymentStatus fromStatus,
            PaymentStatus toStatus,
            String sortBy,
            String sortDirection
    ) {
        StringBuilder query = new StringBuilder("payment.customer.id = ?1");
        java.util.List<Object> parameters = new java.util.ArrayList<>();
        parameters.add(customerId);

        if (paymentId != null) {
            query.append(" and payment.id = ?").append(parameters.size() + 1);
            parameters.add(paymentId);
        }

        if (merchantId != null) {
            query.append(" and payment.merchant.id = ?").append(parameters.size() + 1);
            parameters.add(merchantId);
        }

        if (fromStatus != null) {
            query.append(" and fromStatus = ?").append(parameters.size() + 1);
            parameters.add(fromStatus);
        }

        if (toStatus != null) {
            query.append(" and toStatus = ?").append(parameters.size() + 1);
            parameters.add(toStatus);
        }

        query.append(" order by ")
                .append(resolveSortField(sortBy))
                .append(" ")
                .append(resolveSortDirection(sortDirection));

        return find(query.toString(), parameters.toArray());
    }

    private String resolveSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "createdAt";
        }

        return switch (sortBy) {
            case "amount" -> "amount";
            case "fromStatus" -> "fromStatus";
            case "toStatus" -> "toStatus";
            case "createdAt" -> "createdAt";
            default -> "createdAt";
        };
    }

    private String resolveSortDirection(String sortDirection) {
        if ("asc".equalsIgnoreCase(sortDirection)) {
            return "asc";
        }
        return "desc";
    }
}