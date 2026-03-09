package com.lagrodrigues.moviestore.transaction;

import com.lagrodrigues.moviestore.payment.PaymentStatus;
import com.lagrodrigues.moviestore.transaction.dto.PagedResponse;
import com.lagrodrigues.moviestore.transaction.dto.PaymentTransactionResponse;
import com.lagrodrigues.moviestore.user.User;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

/**
 * Provides transaction history services for merchants and customers.
 */
@ApplicationScoped
public class TransactionService {

    @Inject
    PaymentTransactionRepository paymentTransactionRepository;

    /**
     * Returns a paged list of transaction history entries related to the authenticated merchant's store.
     *
     * @param merchant      the authenticated merchant
     * @param paymentId     the optional payment identifier filter
     * @param customerId    the optional customer identifier filter
     * @param fromStatus    the optional origin status filter
     * @param toStatus      the optional destination status filter
     * @param page          the zero-based page index
     * @param size          the page size
     * @param sortBy        the sort field
     * @param sortDirection the sort direction
     * @return the paged transaction history response
     */
    public PagedResponse<PaymentTransactionResponse> getMerchantTransactions(
            User merchant,
            UUID paymentId,
            UUID customerId,
            PaymentStatus fromStatus,
            PaymentStatus toStatus,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        PanacheQuery<PaymentTransaction> query = paymentTransactionRepository.findMerchantTransactions(
                merchant.id,
                paymentId,
                customerId,
                fromStatus,
                toStatus,
                sortBy,
                sortDirection
        );

        long totalItems = query.count();

        List<PaymentTransactionResponse> items = query.page(page, size)
                .list()
                .stream()
                .map(this::toResponse)
                .toList();

        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalItems / size);

        return new PagedResponse<>(items, page, size, totalItems, totalPages);
    }

    /**
     * Returns a paged list of transaction history entries belonging to the authenticated customer.
     *
     * @param customer      the authenticated customer
     * @param paymentId     the optional payment identifier filter
     * @param merchantId    the optional merchant identifier filter
     * @param fromStatus    the optional origin status filter
     * @param toStatus      the optional destination status filter
     * @param page          the zero-based page index
     * @param size          the page size
     * @param sortBy        the sort field
     * @param sortDirection the sort direction
     * @return the paged transaction history response
     */
    public PagedResponse<PaymentTransactionResponse> getCustomerTransactions(
            User customer,
            UUID paymentId,
            UUID merchantId,
            PaymentStatus fromStatus,
            PaymentStatus toStatus,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        PanacheQuery<PaymentTransaction> query = paymentTransactionRepository.findCustomerTransactions(
                customer.id,
                paymentId,
                merchantId,
                fromStatus,
                toStatus,
                sortBy,
                sortDirection
        );

        long totalItems = query.count();

        List<PaymentTransactionResponse> items = query.page(page, size)
                .list()
                .stream()
                .map(this::toResponse)
                .toList();

        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalItems / size);

        return new PagedResponse<>(items, page, size, totalItems, totalPages);
    }

    /**
     * Maps a payment transaction entity to the API response model.
     *
     * @param transaction the payment transaction entity
     * @return the mapped response
     */
    private PaymentTransactionResponse toResponse(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(
                transaction.id,
                transaction.payment.id,
                transaction.fromStatus,
                transaction.toStatus,
                transaction.amount,
                transaction.message,
                transaction.createdAt
        );
    }
}