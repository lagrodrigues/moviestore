package com.lagrodrigues.moviestore.payment;

import com.lagrodrigues.moviestore.auth.AuthenticatedUser;
import com.lagrodrigues.moviestore.auth.SecurityUtils;
import com.lagrodrigues.moviestore.payment.dto.ConfirmPaymentRequest;
import com.lagrodrigues.moviestore.payment.dto.CreatePaymentRequest;
import com.lagrodrigues.moviestore.payment.dto.PaymentResponse;
import com.lagrodrigues.moviestore.user.User;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

/**
 * Exposes payment-related endpoints for customers and merchants.
 */
@Path("/api/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Payments", description = "Operations related to payments for customers and merchants.")
@SecurityRequirement(name = "apiKey")
public class PaymentResource {

    @Inject
    AuthenticatedUser authenticatedUser;

    @Inject
    SecurityUtils securityUtils;

    @Inject
    PaymentService paymentService;

    /**
     * Creates a new payment for the selected store listing.
     *
     * @param idempotencyKey the idempotency key supplied by the client
     * @param request        the payment creation request
     * @return the created or previously existing payment
     */
    @POST
    @Operation(
            summary = "Create payment",
            description = "Creates a new payment in CREATED status for the selected store listing."
    )
    @APIResponse(responseCode = "200", description = "Payment created successfully.")
    @APIResponse(responseCode = "400", description = "The request is invalid.")
    @APIResponse(responseCode = "401", description = "The request did not contain a valid API key.")
    @APIResponse(responseCode = "403", description = "Customer access is required.")
    @APIResponse(responseCode = "404", description = "Store listing not found.")
    public PaymentResponse createPayment(
            @HeaderParam("Idempotency-Key") String idempotencyKey,
            @Valid CreatePaymentRequest request) {

        User customer = securityUtils.requireCustomer(authenticatedUser);
        Payment payment = paymentService.createPayment(customer, idempotencyKey, request);
        return toResponse(payment);
    }

    /**
     * Confirms an existing payment using simulated card details.
     *
     * @param paymentId the payment identifier
     * @param request   the confirmation request
     * @return the updated payment
     */
    @POST
    @Path("/{paymentId}/confirm")
    @Operation(
            summary = "Confirm payment",
            description = "Confirms an existing payment using simulated card details."
    )
    @APIResponse(responseCode = "200", description = "Payment confirmed successfully.")
    @APIResponse(responseCode = "400", description = "The request is invalid.")
    @APIResponse(responseCode = "401", description = "The request did not contain a valid API key.")
    @APIResponse(responseCode = "403", description = "Customer access is required.")
    @APIResponse(responseCode = "404", description = "Payment not found.")
    @APIResponse(responseCode = "409", description = "Payment has already been processed or cannot be confirmed from its current state.")
    public PaymentResponse confirmPayment(
            @PathParam("paymentId") UUID paymentId,
            @Valid ConfirmPaymentRequest request) {

        User customer = securityUtils.requireCustomer(authenticatedUser);
        Payment payment = paymentService.confirmPayment(customer, paymentId, request);
        return toResponse(payment);
    }

    /**
     * Returns the details of a payment owned by the authenticated customer.
     *
     * @param paymentId the payment identifier
     * @return the requested payment
     */
    @GET
    @Path("/customer/{paymentId}")
    @Operation(
            summary = "Get customer payment",
            description = "Returns the details of a payment owned by the authenticated customer."
    )
    @APIResponse(responseCode = "200", description = "Payment returned successfully.")
    @APIResponse(responseCode = "401", description = "The request did not contain a valid API key.")
    @APIResponse(responseCode = "403", description = "Customer access is required.")
    @APIResponse(responseCode = "404", description = "Payment not found.")
    public PaymentResponse getCustomerPayment(@PathParam("paymentId") UUID paymentId) {
        User customer = securityUtils.requireCustomer(authenticatedUser);
        Payment payment = paymentService.getPayment(customer, paymentId);
        return toResponse(payment);
    }

    /**
     * Returns the details of a payment related to the authenticated merchant's store.
     *
     * @param paymentId the payment identifier
     * @return the requested payment
     */
    @GET
    @Path("/merchant/{paymentId}")
    @Operation(
            summary = "Get merchant payment",
            description = "Returns the details of a payment related to the authenticated merchant's store."
    )
    @APIResponse(responseCode = "200", description = "Payment returned successfully.")
    @APIResponse(responseCode = "401", description = "The request did not contain a valid API key.")
    @APIResponse(responseCode = "403", description = "Merchant access is required.")
    @APIResponse(responseCode = "404", description = "Payment not found.")
    public PaymentResponse getMerchantPayment(@PathParam("paymentId") UUID paymentId) {
        User merchant = securityUtils.requireMerchant(authenticatedUser);
        Payment payment = paymentService.getMerchantPayment(merchant, paymentId);
        return toResponse(payment);
    }

    /**
     * Maps the payment entity to the API response model.
     *
     * @param payment the payment entity
     * @return the mapped response
     */
    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.id,
                payment.storeListing.id,
                payment.customer.id,
                payment.merchant.id,
                payment.externalMovieId,
                payment.movieTitle,
                payment.type.name(),
                payment.amount,
                payment.status,
                payment.failureReason,
                payment.createdAt,
                payment.updatedAt
        );
    }
}