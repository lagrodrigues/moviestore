package com.lagrodrigues.moviestore.transaction;

import com.lagrodrigues.moviestore.auth.AuthenticatedUser;
import com.lagrodrigues.moviestore.auth.SecurityUtils;
import com.lagrodrigues.moviestore.payment.PaymentStatus;
import com.lagrodrigues.moviestore.transaction.dto.PagedResponse;
import com.lagrodrigues.moviestore.transaction.dto.PaymentTransactionResponse;
import com.lagrodrigues.moviestore.user.User;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

/**
 * Exposes transaction history listing endpoints for merchants and customers.
 */
@Path("/api/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Transactions", description = "Operations related to payment transaction history for merchants and customers.")
@SecurityRequirement(name = "apiKey")
public class TransactionResource {

    @Inject
    AuthenticatedUser authenticatedUser;

    @Inject
    SecurityUtils securityUtils;

    @Inject
    TransactionService transactionService;

    /**
     * Returns the transaction history related to the authenticated merchant's store.
     *
     * @param paymentId     the optional payment identifier filter
     * @param customerId    the optional customer identifier filter
     * @param fromStatus    the optional origin status filter
     * @param toStatus      the optional destination status filter
     * @param page          the zero-based page index
     * @param size          the page size
     * @param sortBy        the sort field
     * @param sortDirection the sort direction
     * @return the paged merchant transaction history response
     */
    @GET
    @Path("/merchant")
    @Operation(
            summary = "Get merchant transactions",
            description = "Returns the payment transaction history related to the authenticated merchant's store."
    )
    @APIResponse(responseCode = "200", description = "Transactions returned successfully.")
    @APIResponse(responseCode = "401", description = "The request did not contain a valid API key.")
    @APIResponse(responseCode = "403", description = "Merchant access is required.")
    public PagedResponse<PaymentTransactionResponse> getMerchantTransactions(
            @QueryParam("paymentId") UUID paymentId,
            @QueryParam("customerId") UUID customerId,
            @QueryParam("fromStatus") PaymentStatus fromStatus,
            @QueryParam("toStatus") PaymentStatus toStatus,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sortBy") @DefaultValue("createdAt") String sortBy,
            @QueryParam("sortDirection") @DefaultValue("desc") String sortDirection
    ) {
        User merchant = securityUtils.requireMerchant(authenticatedUser);

        return transactionService.getMerchantTransactions(
                merchant,
                paymentId,
                customerId,
                fromStatus,
                toStatus,
                page,
                size,
                sortBy,
                sortDirection
        );
    }

    /**
     * Returns the transaction history belonging to the authenticated customer.
     *
     * @param paymentId     the optional payment identifier filter
     * @param merchantId    the optional merchant identifier filter
     * @param fromStatus    the optional origin status filter
     * @param toStatus      the optional destination status filter
     * @param page          the zero-based page index
     * @param size          the page size
     * @param sortBy        the sort field
     * @param sortDirection the sort direction
     * @return the paged customer transaction history response
     */
    @GET
    @Path("/customer")
    @Operation(
            summary = "Get customer transactions",
            description = "Returns the payment transaction history belonging to the authenticated customer."
    )
    @APIResponse(responseCode = "200", description = "Transactions returned successfully.")
    @APIResponse(responseCode = "401", description = "The request did not contain a valid API key.")
    @APIResponse(responseCode = "403", description = "Customer access is required.")
    public PagedResponse<PaymentTransactionResponse> getCustomerTransactions(
            @QueryParam("paymentId") UUID paymentId,
            @QueryParam("merchantId") UUID merchantId,
            @QueryParam("fromStatus") PaymentStatus fromStatus,
            @QueryParam("toStatus") PaymentStatus toStatus,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sortBy") @DefaultValue("createdAt") String sortBy,
            @QueryParam("sortDirection") @DefaultValue("desc") String sortDirection
    ) {
        User customer = securityUtils.requireCustomer(authenticatedUser);

        return transactionService.getCustomerTransactions(
                customer,
                paymentId,
                merchantId,
                fromStatus,
                toStatus,
                page,
                size,
                sortBy,
                sortDirection
        );
    }
}