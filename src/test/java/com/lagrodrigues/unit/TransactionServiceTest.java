package com.lagrodrigues.unit;

import com.lagrodrigues.moviestore.payment.Payment;
import com.lagrodrigues.moviestore.payment.PaymentStatus;
import com.lagrodrigues.moviestore.transaction.PaymentTransaction;
import com.lagrodrigues.moviestore.transaction.PaymentTransactionRepository;
import com.lagrodrigues.moviestore.transaction.TransactionService;
import com.lagrodrigues.moviestore.transaction.dto.PagedResponse;
import com.lagrodrigues.moviestore.transaction.dto.PaymentTransactionResponse;
import com.lagrodrigues.moviestore.user.User;
import com.lagrodrigues.moviestore.user.UserRole;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TransactionService}.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    PanacheQuery<PaymentTransaction> paymentTransactionQuery;

    @InjectMocks
    TransactionService transactionService;

    @Test
    void shouldReturnMerchantTransactionsSuccessfully() {
        User merchant = merchant();
        PaymentTransaction transaction = paymentTransaction(merchant, customer());

        when(paymentTransactionRepository.findMerchantTransactions(
                eq(merchant.id),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("createdAt"),
                eq("desc")
        )).thenReturn(paymentTransactionQuery);

        when(paymentTransactionQuery.count()).thenReturn(1L);
        when(paymentTransactionQuery.page(0, 10)).thenReturn(paymentTransactionQuery);
        when(paymentTransactionQuery.list()).thenReturn(List.of(transaction));

        PagedResponse<PaymentTransactionResponse> result = transactionService.getMerchantTransactions(
                merchant,
                null,
                null,
                null,
                null,
                0,
                10,
                "createdAt",
                "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.items().size());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(1L, result.totalItems());
        assertEquals(1, result.totalPages());

        PaymentTransactionResponse item = result.items().getFirst();
        assertEquals(transaction.id, item.id());
        assertEquals(transaction.payment.id, item.paymentId());
        assertEquals(transaction.fromStatus, item.fromStatus());
        assertEquals(transaction.toStatus, item.toStatus());
        assertEquals(transaction.amount, item.amount());
        assertEquals(transaction.message, item.message());
        assertEquals(transaction.createdAt, item.createdAt());
    }

    @Test
    void shouldReturnCustomerTransactionsSuccessfully() {
        User customer = customer();
        User merchant = merchant();
        PaymentTransaction transaction = paymentTransaction(merchant, customer);

        when(paymentTransactionRepository.findCustomerTransactions(
                eq(customer.id),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("createdAt"),
                eq("desc")
        )).thenReturn(paymentTransactionQuery);

        when(paymentTransactionQuery.count()).thenReturn(1L);
        when(paymentTransactionQuery.page(0, 10)).thenReturn(paymentTransactionQuery);
        when(paymentTransactionQuery.list()).thenReturn(List.of(transaction));

        PagedResponse<PaymentTransactionResponse> result = transactionService.getCustomerTransactions(
                customer,
                null,
                null,
                null,
                null,
                0,
                10,
                "createdAt",
                "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.items().size());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(1L, result.totalItems());
        assertEquals(1, result.totalPages());

        PaymentTransactionResponse item = result.items().getFirst();
        assertEquals(transaction.id, item.id());
        assertEquals(transaction.payment.id, item.paymentId());
        assertEquals(transaction.fromStatus, item.fromStatus());
        assertEquals(transaction.toStatus, item.toStatus());
        assertEquals(transaction.amount, item.amount());
        assertEquals(transaction.message, item.message());
        assertEquals(transaction.createdAt, item.createdAt());
    }

    @Test
    void shouldCalculateMultiplePagesCorrectlyForMerchantTransactions() {
        User merchant = merchant();

        when(paymentTransactionRepository.findMerchantTransactions(
                eq(merchant.id),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("createdAt"),
                eq("desc")
        )).thenReturn(paymentTransactionQuery);

        when(paymentTransactionQuery.count()).thenReturn(25L);
        when(paymentTransactionQuery.page(1, 10)).thenReturn(paymentTransactionQuery);
        when(paymentTransactionQuery.list()).thenReturn(List.of());

        PagedResponse<PaymentTransactionResponse> result = transactionService.getMerchantTransactions(
                merchant,
                null,
                null,
                null,
                null,
                1,
                10,
                "createdAt",
                "desc"
        );

        assertEquals(25L, result.totalItems());
        assertEquals(3, result.totalPages());
        assertEquals(1, result.page());
        assertEquals(10, result.size());
        assertTrue(result.items().isEmpty());
    }

    @Test
    void shouldCalculateZeroTotalPagesWhenSizeIsZero() {
        User customer = customer();

        when(paymentTransactionRepository.findCustomerTransactions(
                eq(customer.id),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("createdAt"),
                eq("desc")
        )).thenReturn(paymentTransactionQuery);

        when(paymentTransactionQuery.count()).thenReturn(5L);
        when(paymentTransactionQuery.page(0, 0)).thenReturn(paymentTransactionQuery);
        when(paymentTransactionQuery.list()).thenReturn(List.of());

        PagedResponse<PaymentTransactionResponse> result = transactionService.getCustomerTransactions(
                customer,
                null,
                null,
                null,
                null,
                0,
                0,
                "createdAt",
                "desc"
        );

        assertEquals(0, result.totalPages());
        assertEquals(5L, result.totalItems());
        assertTrue(result.items().isEmpty());
    }

    private User merchant() {
        User user = new User();
        user.id = UUID.randomUUID();
        user.username = "merchant";
        user.role = UserRole.MERCHANT;
        user.createdAt = OffsetDateTime.now();
        return user;
    }

    private User customer() {
        User user = new User();
        user.id = UUID.randomUUID();
        user.username = "customer";
        user.role = UserRole.CUSTOMER;
        user.createdAt = OffsetDateTime.now();
        return user;
    }

    private PaymentTransaction paymentTransaction(User merchant, User customer) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.customer = customer;
        payment.merchant = merchant;

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.id = UUID.randomUUID();
        transaction.payment = payment;
        transaction.fromStatus = PaymentStatus.CREATED;
        transaction.toStatus = PaymentStatus.PROCESSING;
        transaction.amount = new BigDecimal("9.99");
        transaction.message = "Payment confirmation is being processed.";
        transaction.createdAt = OffsetDateTime.now();

        return transaction;
    }
}