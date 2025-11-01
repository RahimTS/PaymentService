package rahim.learning.paymentservices.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rahim.learning.paymentservices.dtos.PaymentRequestDto;
import rahim.learning.paymentservices.dtos.PaymentResponseDto;
import rahim.learning.paymentservices.dtos.PaymentStatusUpdateDto;
import rahim.learning.paymentservices.entities.Payment;
import rahim.learning.paymentservices.entities.PaymentEvent;
import rahim.learning.paymentservices.entities.PaymentGatewayType;
import rahim.learning.paymentservices.entities.PaymentStatus;
import rahim.learning.paymentservices.exceptions.*;
import rahim.learning.paymentservices.metrics.PaymentMetrics;
import rahim.learning.paymentservices.paymentgateways.IPaymentGateway;
import rahim.learning.paymentservices.paymentgateways.PaymentGatewayStrategy;
import rahim.learning.paymentservices.repositories.PaymentEventRepository;
import rahim.learning.paymentservices.repositories.PaymentRepository;
import rahim.learning.paymentservices.services.IdempotencyService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Production-ready payment service implementation.
 * 
 * Features:
 * - Idempotency handling
 * - Event sourcing for audit trail
 * - Retry logic with exponential backoff
 * - Transactional integrity
 * - Gateway integration with error handling
 */
@Service
@Slf4j
public class PaymentService implements IPaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentEventRepository eventRepository;

    @Autowired
    private PaymentGatewayStrategy gatewayStrategy;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private PaymentMetrics paymentMetrics;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int PAYMENT_LINK_EXPIRY_HOURS = 24;

    @Override
    @Transactional
    @Cacheable(
        value = "payments",
        key = "#requestDto.idempotencyKey != null && #requestDto.idempotencyKey != '' ? #requestDto.idempotencyKey : #requestDto.orderId",
        unless = "#result == null"
    )
    public PaymentResponseDto createPayment(PaymentRequestDto requestDto) {
        log.info("Creating payment for order: {}", requestDto.getOrderId());

        // Start timer for processing duration metric
        long startTime = System.currentTimeMillis();

        // 1. Generate or use provided idempotency key
        String idempotencyKey = generateIdempotencyKey(requestDto);

        // 2a. Fast-path idempotency using Redis (if previously processed)
        if (idempotencyService.isProcessed(idempotencyKey)) {
            String existingId = idempotencyService.getPaymentId(idempotencyKey);
            if (existingId != null) {
                try {
                    UUID pid = UUID.fromString(existingId);
                    Optional<Payment> byId = paymentRepository.findById(pid);
                    if (byId.isPresent()) {
                        log.info("Returning cached payment from Redis idempotency for key: {}", idempotencyKey);
                        // Record idempotency cache hit (Redis)
                        paymentMetrics.recordIdempotencyCacheHit();
                        return mapToResponseDto(byId.get());
                    }
                } catch (IllegalArgumentException ignored) {
                    // Fallback to DB lookup by idempotencyKey
                }
            }
        }

        // 2b. Check for existing payment in DB (idempotency)
        Optional<Payment> existingPayment = paymentRepository
                .findByIdempotencyKey(idempotencyKey);

        if (existingPayment.isPresent()) {
            log.info("Returning existing payment for idempotency key: {}", idempotencyKey);
            // Record idempotency cache hit (DB)
            paymentMetrics.recordIdempotencyCacheHit();
            return mapToResponseDto(existingPayment.get());
        }

        // Record idempotency cache miss (neither Redis nor DB had it)
        paymentMetrics.recordIdempotencyCacheMiss();

        // 3. Check if order already has a payment
        if (paymentRepository.existsByOrderId(requestDto.getOrderId())) {
            Payment existing = paymentRepository.findByOrderId(requestDto.getOrderId())
                    .orElseThrow();
            throw new DuplicatePaymentException(
                    "Payment already exists for order: " + requestDto.getOrderId(),
                    existing.getId().toString());
        }

        // 4. Create payment entity
        Payment payment = buildPaymentEntity(requestDto, idempotencyKey);
    payment = paymentRepository.save(payment);

    // Record payment created metric and active processing
    paymentMetrics.recordPaymentCreated(payment.getGatewayType(), payment.getAmount());
    paymentMetrics.incrementActivePayments();

        // 5. Record initialization event
        recordEvent(payment.getId(), "PAYMENT_INITIATED",
                String.format("Payment initiated for order %s with amount %s %s",
                        payment.getOrderId(), payment.getAmount(), payment.getCurrency()));

        // 6. Create payment link with gateway
        try {
            String paymentLink = createPaymentLinkWithRetry(payment, requestDto);

            payment.setPaymentLink(paymentLink);
            payment.setStatus(PaymentStatus.PENDING);
            payment = paymentRepository.save(payment);

            recordEvent(payment.getId(), "PAYMENT_LINK_CREATED",
                    "Payment link created successfully");

    log.info("Payment created successfully. ID: {}, Order: {}",
                    payment.getId(), payment.getOrderId());

        // Mark idempotency key as processed in Redis for fast subsequent lookups
        idempotencyService.markAsProcessed(idempotencyKey, payment.getId());

        // Record success metrics
        Duration processingTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
        paymentMetrics.recordPaymentSuccess(
            payment.getGatewayType(),
            payment.getAmount(),
            processingTime
        );
        paymentMetrics.decrementActivePayments();

            return mapToResponseDto(payment);

        } catch (Exception e) {
            log.error("Failed to create payment link for order: {}",
                    payment.getOrderId(), e);

            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(e.getMessage());
            payment.setErrorCode("GATEWAY_ERROR");
            paymentRepository.save(payment);

        recordEvent(payment.getId(), "PAYMENT_FAILED",
                    "Failed to create payment link: " + e.getMessage());

        // Record failure metrics
        paymentMetrics.recordPaymentFailure(
            payment.getGatewayType(),
            "GATEWAY_ERROR",
            e.getMessage()
        );
        paymentMetrics.decrementActivePayments();

            throw new PaymentProcessingException(
                    "Failed to create payment link: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentById(UUID paymentId) {
        log.debug("Fetching payment by ID: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        paymentId.toString(), "id"));

        return mapToResponseDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentByOrderId(String orderId) {
        log.debug("Fetching payment by order ID: {}", orderId);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        orderId, "orderId"));

        return mapToResponseDto(payment);
    }

    @Override
    @Transactional
    public PaymentResponseDto updatePaymentStatus(
            UUID paymentId,
            PaymentStatusUpdateDto statusUpdate) {
        log.info("Updating payment {} status to: {}", paymentId, statusUpdate.getStatus());

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        paymentId.toString(), "id"));

        // Validate state transition
        validateStateTransition(payment, statusUpdate.getStatus());

        PaymentStatus oldStatus = payment.getStatus();
        payment.setStatus(statusUpdate.getStatus());

        if (statusUpdate.getGatewayPaymentId() != null) {
            payment.setGatewayPaymentId(statusUpdate.getGatewayPaymentId());
        }

        if (statusUpdate.getErrorMessage() != null) {
            payment.setErrorMessage(statusUpdate.getErrorMessage());
            payment.setErrorCode(statusUpdate.getErrorCode());
        }

        // Set completion timestamp for terminal success states
        if (statusUpdate.getStatus() == PaymentStatus.SUCCESS ||
                statusUpdate.getStatus() == PaymentStatus.CAPTURED) {
            payment.setCompletedAt(LocalDateTime.now());
        }

        payment = paymentRepository.save(payment);

        recordEvent(paymentId, "STATUS_CHANGED",
                String.format("Status changed from %s to %s", oldStatus, statusUpdate.getStatus()));

        log.info("Payment {} status updated successfully", paymentId);

        return mapToResponseDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getPaymentsByCustomer(String customerEmail) {
        log.debug("Fetching payments for customer: {}", customerEmail);

        List<Payment> payments = paymentRepository
                .findByCustomerEmailOrderByCreatedAtDesc(customerEmail);

        return payments.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentResponseDto retryPayment(UUID paymentId) {
        log.info("Retrying payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        paymentId.toString(), "id"));

        // Validate payment can be retried
        if (!payment.canRetry()) {
            throw new InvalidPaymentStateException(
                    payment.getStatus().toString(),
                    "retry");
        }

        payment.setRetryCount(payment.getRetryCount() + 1);
        payment.setLastRetryAt(LocalDateTime.now());

    // Record retry metric
    paymentMetrics.recordPaymentRetry(
        payment.getGatewayType(),
        payment.getRetryCount()
    );

        try {
            // Recreate payment link
            IPaymentGateway gateway = gatewayStrategy.getPaymentGateway(
                    payment.getGatewayType().toString());

            String paymentLink = gateway.createStandardPaymentLink(
                    payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue(),
                    payment.getOrderId(),
                    payment.getCustomerPhone(),
                    payment.getCustomerName(),
                    payment.getCustomerEmail());

            payment.setPaymentLink(paymentLink);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setErrorMessage(null);
            payment.setErrorCode(null);

            payment = paymentRepository.save(payment);

            recordEvent(paymentId, "PAYMENT_RETRIED",
                    String.format("Payment retry attempt #%d", payment.getRetryCount()));

            log.info("Payment {} retried successfully", paymentId);

            return mapToResponseDto(payment);

        } catch (Exception e) {
            log.error("Payment retry failed for ID: {}", paymentId, e);

            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(e.getMessage());
            paymentRepository.save(payment);

            throw new PaymentProcessingException(
                    "Payment retry failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public PaymentResponseDto cancelPayment(UUID paymentId) {
        log.info("Cancelling payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        paymentId.toString(), "id"));

        // Can only cancel pending payments
        if (payment.isTerminal()) {
            throw new InvalidPaymentStateException(
                    payment.getStatus().toString(),
                    "cancel");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment = paymentRepository.save(payment);

        recordEvent(paymentId, "PAYMENT_CANCELLED", "Payment cancelled by user/merchant");

        log.info("Payment {} cancelled successfully", paymentId);

        return mapToResponseDto(payment);
    }

    // ========== Private Helper Methods ==========

    /**
     * Create payment link with retry logic
     */
    @Retryable(value = {
            PaymentProcessingException.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    private String createPaymentLinkWithRetry(Payment payment, PaymentRequestDto requestDto) {
        // Get gateway - handles null gatewayType by using default
        IPaymentGateway gateway = gatewayStrategy.getPaymentGateway(
                requestDto.getGatewayType());

        // Convert amount to smallest currency unit (cents/paise)
        long amountInSmallestUnit = payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        String paymentLink = gateway.createStandardPaymentLink(
                amountInSmallestUnit,
                payment.getOrderId(),
                payment.getCustomerPhone(),
                payment.getCustomerName(),
                payment.getCustomerEmail());

        payment.setRetryCount(payment.getRetryCount() + 1);
        return paymentLink;
    }

    /**
     * Build Payment entity from request DTO
     */
    private Payment buildPaymentEntity(PaymentRequestDto requestDto, String idempotencyKey) {
        // Determine gateway type - use request type if provided, otherwise default
        PaymentGatewayType gatewayType;
        if (requestDto.getGatewayType() != null && !requestDto.getGatewayType().isBlank()) {
            gatewayType = gatewayStrategy.getPaymentGatewayType(requestDto.getGatewayType());
        } else {
            gatewayType = gatewayStrategy.getPaymentGatewayType();
        }

        return Payment.builder()
                .orderId(requestDto.getOrderId())
                .idempotencyKey(idempotencyKey)
                .amount(requestDto.getAmount())
                .currency(requestDto.getCurrency())
                .status(PaymentStatus.INITIATED)
                .gatewayType(gatewayType) // Now uses the determined gateway type
                .customerName(requestDto.getCustomerName())
                .customerEmail(requestDto.getCustomerEmail())
                .customerPhone(requestDto.getCustomerPhone())
                .metadata(requestDto.getDescription())
                .expiresAt(LocalDateTime.now().plusHours(PAYMENT_LINK_EXPIRY_HOURS))
                .retryCount(0)
                .build();
    }

    /**
     * Generate idempotency key
     */
    private String generateIdempotencyKey(PaymentRequestDto requestDto) {
        if (requestDto.getIdempotencyKey() != null &&
                !requestDto.getIdempotencyKey().isBlank()) {
            return requestDto.getIdempotencyKey();
        }
        return requestDto.getOrderId() + "-" + UUID.randomUUID().toString();
    }

    /**
     * Record payment event for audit trail
     */
    private void recordEvent(UUID paymentId, String eventType, String message) {
        try {
            PaymentEvent event = PaymentEvent.builder()
                    .paymentId(paymentId)
                    .eventType(eventType)
                    .payload(message)
                    .source("API")
                    .triggeredBy("SYSTEM")
                    .build();

            eventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to record event for payment: {}", paymentId, e);
            // Don't fail the payment operation if event recording fails
        }
    }

    /**
     * Validate state transition is allowed
     */
    private void validateStateTransition(Payment payment, PaymentStatus newStatus) {
        // Terminal states cannot be changed
        if (payment.isTerminal()) {
            throw new InvalidPaymentStateException(
                    payment.getStatus().toString(),
                    "update to " + newStatus);
        }

        // Add more specific validation rules as needed
        PaymentStatus currentStatus = payment.getStatus();

        // Example: Cannot go from FAILED to SUCCESS without retry
        if (currentStatus == PaymentStatus.FAILED &&
                newStatus == PaymentStatus.SUCCESS) {
            throw new InvalidPaymentStateException(
                    currentStatus.toString(),
                    "direct transition to SUCCESS");
        }
    }

    /**
     * Map Payment entity to Response DTO
     */
    private PaymentResponseDto mapToResponseDto(Payment payment) {
        return PaymentResponseDto.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .gatewayType(payment.getGatewayType())
                .paymentLink(payment.getPaymentLink())
                .gatewayPaymentId(payment.getGatewayPaymentId())
                .customerName(payment.getCustomerName())
                .customerEmail(maskEmail(payment.getCustomerEmail()))
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .completedAt(payment.getCompletedAt())
                .expiresAt(payment.getExpiresAt())
                .errorMessage(payment.getErrorMessage())
                .errorCode(payment.getErrorCode())
                .retryCount(payment.getRetryCount())
                .canRetry(payment.canRetry())
                .isExpired(payment.isExpired())
                .isSuccessful(payment.isSuccessful())
                .message(generateStatusMessage(payment))
                .build();
    }

    /**
     * Mask email for privacy (show only first 2 chars and domain)
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String masked = localPart.length() > 2
                ? localPart.substring(0, 2) + "***"
                : "***";
        return masked + "@" + parts[1];
    }

    /**
     * Generate user-friendly status message
     */
    private String generateStatusMessage(Payment payment) {
        return switch (payment.getStatus()) {
            case INITIATED -> "Payment is being initialized";
            case PENDING -> "Payment link is ready. Awaiting customer action";
            case PROCESSING -> "Payment is being processed";
            case AUTHORIZED -> "Payment authorized. Awaiting capture";
            case CAPTURED, SUCCESS -> "Payment completed successfully";
            case FAILED -> "Payment failed: " +
                    (payment.getErrorMessage() != null ? payment.getErrorMessage() : "Unknown error");
            case EXPIRED -> "Payment link has expired";
            case CANCELLED -> "Payment was cancelled";
            case REFUNDED -> "Payment has been refunded";
            case PARTIALLY_REFUNDED -> "Payment has been partially refunded";
        };
    }
}
