package rahim.learning.paymentservices.controllers;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rahim.learning.paymentservices.dtos.PaymentRequestDto;
import rahim.learning.paymentservices.dtos.PaymentResponseDto;
import rahim.learning.paymentservices.dtos.PaymentStatusUpdateDto;
import rahim.learning.paymentservices.exceptions.GlobalExceptionHandler;
import rahim.learning.paymentservices.services.IPaymentService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for payment operations.
 * Provides comprehensive payment management APIs with full OpenAPI documentation.
 * 
 * @author Rahim T S
 * @version 1.3.0
 */
@RestController
@RequestMapping("/api/v1/payments")
@Slf4j
@Tag(
    name = "Payment Management",
    description = "APIs for creating, retrieving, and managing payment transactions. " +
                  "Supports multiple payment gateways (Stripe, Razorpay) with idempotency, " +
                  "retry capabilities, and comprehensive error handling."
)
public class PaymentController {

    @Autowired
    private IPaymentService paymentService;

    /**
     * Create a new payment
     * 
     * POST /api/v1/payments
     * 
     * @param requestDto Payment creation request with customer and amount details
     * @param idempotencyKey Optional idempotency key to prevent duplicate payments
     * @return Created payment with payment link (HTTP 201)
     */
    @Operation(
        summary = "Create a new payment",
        description = """
            Creates a new payment transaction and generates a payment link for the customer.
            
            **Features:**
            - Multi-gateway support (Stripe, Razorpay)
            - Automatic payment link generation with 24-hour validity
            - Idempotency support via header or request body
            - Event sourcing for complete audit trail
            - Automatic retry handling for gateway failures
            
            **Idempotency:**
            To prevent duplicate payments, provide an `Idempotency-Key` header.
            If the same key is used again, the original payment will be returned 
            instead of creating a new one.
            
            **Payment Link Validity:**
            Generated payment links are valid for 24 hours. After expiration,
            the payment status will be automatically marked as EXPIRED.
            
            **Example Request:**
            ```
            {
              "orderId": "ORD-12345",
              "amount": 99.99,
              "currency": "USD",
              "customerName": "John Doe",
              "customerEmail": "john@example.com",
              "customerPhone": "+919876543210",
              "description": "Product purchase"
            }
            ```
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Payment created successfully. Payment link generated and ready for customer.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters. Check validation errors in response.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Duplicate payment detected. Order already has an existing payment.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "502",
            description = "Payment gateway error. Unable to create payment link. Will be automatically retried.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class)
            )
        )
    })
    @PostMapping
    @Timed(value = "payments.create", description = "Time taken to create payment")
    public ResponseEntity<PaymentResponseDto> createPayment(
        @Valid @RequestBody
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Payment creation request containing customer details, amount, and order information",
            required = true
        )
        PaymentRequestDto requestDto,
        
        @RequestHeader(value = "Idempotency-Key", required = false)
        @Parameter(
            description = """
                Optional idempotency key to prevent duplicate payment creation.
                Recommended format: {orderId}-{timestamp} or client-generated UUID.
                Same key will return existing payment instead of creating new one.
                Keys are valid for 24 hours.
                """,
            example = "ORD-12345-1698307200000"
        )
        String idempotencyKey
    ) {
        log.info("Received payment creation request for order: {}", requestDto.getOrderId());
        
        // Use header idempotency key if provided
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            requestDto.setIdempotencyKey(idempotencyKey);
        }
        
        PaymentResponseDto response = paymentService.createPayment(requestDto);
        
        log.info("Payment created successfully. ID: {}, Order: {}", 
                response.getPaymentId(), response.getOrderId());
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }
    
    /**
     * Get payment by UUID
     * 
     * GET /api/v1/payments/{paymentId}
     * 
     * @param paymentId Payment UUID
     * @return Payment details (HTTP 200)
     */
    @Operation(
        summary = "Get payment by UUID",
        description = """
            Retrieves complete payment details using the payment UUID.
            
            **Returns:**
            - Payment status and lifecycle information
            - Payment link (if still valid)
            - Customer details (email masked for privacy)
            - Gateway information
            - Transaction timestamps
            - Retry information (if applicable)
            
            **Use Cases:**
            - Check payment status after creation
            - Retrieve payment link for customer
            - Monitor payment progress
            - Audit and reconciliation
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Payment found and returned successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment not found with given UUID",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class)
            )
        )
    })
    @GetMapping("/{paymentId}")
    @Timed(value = "payments.get", description = "Time taken to fetch payment")
    public ResponseEntity<PaymentResponseDto> getPaymentById(
        @PathVariable
        @Parameter(
            description = "Payment UUID (unique identifier)",
            required = true,
            example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID paymentId
    ) {
        log.debug("Fetching payment: {}", paymentId);
        
        PaymentResponseDto response = paymentService.getPaymentById(paymentId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get payment by order ID
     * 
     * GET /api/v1/payments/order/{orderId}
     * 
     * @param orderId Merchant order ID
     * @return Payment details (HTTP 200)
     */
    @Operation(
        summary = "Get payment by order ID",
        description = """
            Retrieves payment using your merchant order ID.
            
            **Use Cases:**
            - Check payment status from your order system
            - Verify payment for order fulfillment
            - Display payment status to customer
            - Order reconciliation
            
            **Note:**
            Each order can have only one payment. If multiple payment attempts
            were made, this returns the most recent valid payment.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Payment found for given order ID",
            content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No payment found for this order ID",
            content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class))
        )
    })
    @GetMapping("/order/{orderId}")
    @Timed(value = "payments.getByOrder", description = "Time taken to fetch payment by order")
    public ResponseEntity<PaymentResponseDto> getPaymentByOrderId(
        @PathVariable
        @Parameter(
            description = "Merchant order ID (from your system)",
            required = true,
            example = "ORD-12345"
        )
        String orderId
    ) {
        log.debug("Fetching payment for order: {}", orderId);
        
        PaymentResponseDto response = paymentService.getPaymentByOrderId(orderId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all payments for a customer
     * 
     * GET /api/v1/payments/customer/{email}
     * 
     * @param email Customer email address
     * @return List of payments (HTTP 200)
     */
    @Operation(
        summary = "Get customer payment history",
        description = """
            Retrieves all payments for a specific customer email address.
            Results are returned in descending order (newest first).
            
            **Returns:**
            - All payments associated with the email
            - Complete payment lifecycle for each transaction
            - Empty list if no payments found (not an error)
            
            **Use Cases:**
            - Customer payment history display
            - Account statements
            - Refund processing
            - Customer support
            - Transaction analysis
            
            **Privacy:**
            Email addresses in responses are masked for security.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Payment history retrieved successfully (may be empty list)",
            content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))
        )
    })
    @GetMapping("/customer/{email}")
    @Timed(value = "payments.getByCustomer", description = "Time taken to fetch customer payments")
    public ResponseEntity<List<PaymentResponseDto>> getPaymentsByCustomer(
        @PathVariable
        @Parameter(
            description = "Customer email address",
            required = true,
            example = "customer@example.com"
        )
        String email
    ) {
        log.debug("Fetching payments for customer: {}", email);
        
        List<PaymentResponseDto> payments = paymentService.getPaymentsByCustomer(email);
        
        return ResponseEntity.ok(payments);
    }
    
    /**
     * Update payment status
     * 
     * PATCH /api/v1/payments/{paymentId}/status
     * 
     * @param paymentId Payment UUID
     * @param statusUpdate Status update request
     * @return Updated payment (HTTP 200)
     */
    @Operation(
        summary = "Update payment status",
        description = """
            Updates the status of a payment transaction.
            
            **Typical Users:**
            - Webhook handlers (automatic status updates from gateway)
            - Admin/support operations (manual reconciliation)
            - Background jobs (timeout handling)
            
            **State Transition Rules:**
            - Terminal states (SUCCESS, FAILED, CANCELLED) cannot be changed
            - Must follow valid state machine transitions
            - Invalid transitions will return 400 error
            
            **Valid Transitions:**
            ```
            INITIATED -> PENDING -> PROCESSING -> SUCCESS
                      -> FAILED
                      -> CANCELLED
            PENDING -> EXPIRED (automatic after 24h)
            SUCCESS -> REFUNDED (via separate refund API)
            ```
            
            **Note:**
            Most status updates are automatic via webhooks.
            Manual updates should be done with caution and proper authorization.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Payment status updated successfully",
            content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid state transition or payment already in terminal state",
            content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment not found",
            content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class))
        )
    })
    @PatchMapping("/{paymentId}/status")
    @Timed(value = "payments.updateStatus", description = "Time taken to update payment status")
    public ResponseEntity<PaymentResponseDto> updatePaymentStatus(
        @PathVariable
        @Parameter(
            description = "Payment UUID to update",
            required = true
        )
        UUID paymentId,
        
        @Valid @RequestBody
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Status update request with new status and optional error details",
            required = true
        )
        PaymentStatusUpdateDto statusUpdate
    ) {
        log.info("Updating payment {} status to: {}", paymentId, statusUpdate.getStatus());
        
        PaymentResponseDto response = paymentService.updatePaymentStatus(paymentId, statusUpdate);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Retry a failed payment
     * 
     * POST /api/v1/payments/{paymentId}/retry
     * 
     * @param paymentId Payment UUID to retry
     * @return Updated payment with new payment link (HTTP 200)
     */
    @Operation(
        summary = "Retry a failed payment",
        description = """
            Attempts to retry a failed payment by regenerating the payment link.
            
            **Requirements:**
            - Payment must be in FAILED status
            - Retry count must be less than 3 (maximum attempts)
            - Payment must be created within last 24 hours
            
            **What Happens:**
            1. Validates payment is eligible for retry
            2. Increments retry counter
            3. Generates new payment link via gateway
            4. Updates status to PENDING
            5. Clears previous error messages
            6. Records retry event in audit trail
            
            **Retry Limits:**
            - Maximum 3 retry attempts per payment
            - 24-hour window for retries
            - Exponential backoff between retries
            
            **Use Cases:**
            - Temporary gateway failures
            - Network timeouts
            - Manual retry by customer support
            - Automated retry jobs for recoverable errors
            
            **Note:**
            Successful retry generates a new payment link.
            Customer must use the new link to complete payment.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Payment retried successfully. New payment link generated.",
            content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Payment cannot be retried (not in FAILED state, max retries reached, or expired)",
            content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment not found",
            content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "502",
            description = "Payment gateway error during retry. Will be queued for automatic retry.",
            content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class))
        )
    })
    @PostMapping("/{paymentId}/retry")
    @Timed(value = "payments.retry", description = "Time taken to retry payment")
    public ResponseEntity<PaymentResponseDto> retryPayment(
        @PathVariable
        @Parameter(
            description = "Payment UUID to retry",
            required = true
        )
        UUID paymentId
    ) {
        log.info("Retrying payment: {}", paymentId);
        
        PaymentResponseDto response = paymentService.retryPayment(paymentId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cancel a pending payment
     * 
     * POST /api/v1/payments/{paymentId}/cancel
     * 
     * @param paymentId Payment UUID to cancel
     * @return Cancelled payment (HTTP 200)
     */
    @Operation(
        summary = "Cancel a pending payment",
        description = """
            Cancels a payment that is still in progress.
            Only non-terminal payments can be cancelled.
            
            **Cancellable States:**
            - INITIATED - Payment record created but not sent to gateway
            - PENDING - Payment link generated, awaiting customer
            - PROCESSING - Payment being processed by gateway
            
            **Non-Cancellable States (Terminal):**
            - SUCCESS - Payment completed successfully
            - CAPTURED - Payment amount captured
            - FAILED - Payment already failed
            - EXPIRED - Payment link already expired
            - CANCELLED - Payment already cancelled
            - REFUNDED - Payment already refunded
            
            **What Happens:**
            1. Validates payment can be cancelled
            2. Updates status to CANCELLED
            3. Invalidates payment link
            4. Records cancellation event
            5. Sends cancellation notification (if configured)
            
            **Use Cases:**
            - Customer requests cancellation
            - Order cancelled by merchant
            - Fraudulent transaction detected
            - Customer changed payment method
            - Duplicate order detection
            
            **Note:**
            Once cancelled, the payment cannot be resumed.
            A new payment must be created if the transaction is still needed.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Payment cancelled successfully. Payment link is now invalid.",
            content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Payment cannot be cancelled (already in terminal state)",
            content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment not found",
            content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class))
        )
    })
    @PostMapping("/{paymentId}/cancel")
    @Timed(value = "payments.cancel", description = "Time taken to cancel payment")
    public ResponseEntity<PaymentResponseDto> cancelPayment(
        @PathVariable
        @Parameter(
            description = "Payment UUID to cancel",
            required = true
        )
        UUID paymentId
    ) {
        log.info("Cancelling payment: {}", paymentId);
        
        PaymentResponseDto response = paymentService.cancelPayment(paymentId);
        
        return ResponseEntity.ok(response);
    }
}