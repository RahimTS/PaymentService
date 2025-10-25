package rahim.learning.paymentservices.controllers;

import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rahim.learning.paymentservices.dtos.PaymentRequestDto;
import rahim.learning.paymentservices.dtos.PaymentResponseDto;
import rahim.learning.paymentservices.dtos.PaymentStatusUpdateDto;
import rahim.learning.paymentservices.services.IPaymentService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for payment operations.
 * Provides endpoints for payment creation, retrieval, and management.
 */
@RestController
@RequestMapping("/api/v1/payments")
@Slf4j
public class PaymentController {

    @Autowired
    private IPaymentService paymentService;

    /**
     * Create a new payment
     * 
     * POST /api/v1/payments
     * 
     * @param requestDto Payment creation request
     * @param idempotencyKey Optional idempotency key header
     * @return Created payment with payment link
     */
    @PostMapping
    @Timed(value = "payments.create", description = "Time taken to create payment")
    public ResponseEntity<PaymentResponseDto> createPayment(
        @Valid @RequestBody PaymentRequestDto requestDto,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
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
     * Get payment by ID
     * 
     * GET /api/v1/payments/{paymentId}
     */
    @GetMapping("/{paymentId}")
    @Timed(value = "payments.get", description = "Time taken to fetch payment")
    public ResponseEntity<PaymentResponseDto> getPaymentById(
        @PathVariable UUID paymentId
    ) {
        log.debug("Fetching payment: {}", paymentId);
        
        PaymentResponseDto response = paymentService.getPaymentById(paymentId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get payment by order ID
     * 
     * GET /api/v1/payments/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    @Timed(value = "payments.getByOrder", description = "Time taken to fetch payment by order")
    public ResponseEntity<PaymentResponseDto> getPaymentByOrderId(
        @PathVariable String orderId
    ) {
        log.debug("Fetching payment for order: {}", orderId);
        
        PaymentResponseDto response = paymentService.getPaymentByOrderId(orderId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all payments for a customer
     * 
     * GET /api/v1/payments/customer/{email}
     */
    @GetMapping("/customer/{email}")
    public ResponseEntity<List<PaymentResponseDto>> getPaymentsByCustomer(
        @PathVariable String email
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
     * Note: This endpoint is typically used by internal services or webhooks
     */
    @PatchMapping("/{paymentId}/status")
    public ResponseEntity<PaymentResponseDto> updatePaymentStatus(
        @PathVariable UUID paymentId,
        @Valid @RequestBody PaymentStatusUpdateDto statusUpdate
    ) {
        log.info("Updating payment {} status to: {}", paymentId, statusUpdate.getStatus());
        
        PaymentResponseDto response = paymentService.updatePaymentStatus(paymentId, statusUpdate);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Retry failed payment
     * 
     * POST /api/v1/payments/{paymentId}/retry
     */
    @PostMapping("/{paymentId}/retry")
    public ResponseEntity<PaymentResponseDto> retryPayment(
        @PathVariable UUID paymentId
    ) {
        log.info("Retrying payment: {}", paymentId);
        
        PaymentResponseDto response = paymentService.retryPayment(paymentId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cancel pending payment
     * 
     * POST /api/v1/payments/{paymentId}/cancel
     */
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentResponseDto> cancelPayment(
        @PathVariable UUID paymentId
    ) {
        log.info("Cancelling payment: {}", paymentId);
        
        PaymentResponseDto response = paymentService.cancelPayment(paymentId);
        
        return ResponseEntity.ok(response);
    }
}
