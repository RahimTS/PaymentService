package rahim.learning.paymentservices.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment entity representing a payment transaction in the system.
 * This is the aggregate root for payment domain.
 * 
 * Supports idempotency, audit trail, and event sourcing.
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_order_id", columnList = "order_id"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_gateway_payment_id", columnList = "gateway_payment_id"),
        @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_customer_email", columnList = "customer_email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "metadata" })
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * External order/transaction ID from merchant system
     */
    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    /**
     * Idempotency key to prevent duplicate payments
     * Generated from order_id + unique suffix or provided by client
     */
    @Column(name = "idempotency_key", unique = true, nullable = false, length = 255)
    private String idempotencyKey;

    /**
     * Payment amount in smallest currency unit (e.g., cents for USD, paise for INR)
     * Stored as BigDecimal for precision in financial calculations
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * ISO 4217 currency code (e.g., INR, USD, EUR)
     */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /**
     * Current status of the payment
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    /**
     * Payment gateway used for this transaction
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_type", nullable = false, length = 20)
    private PaymentGatewayType gatewayType;

    /**
     * Payment ID from the gateway (e.g., Stripe payment intent ID)
     */
    @Column(name = "gateway_payment_id", length = 255)
    private String gatewayPaymentId;

    /**
     * Generated payment link for customer
     */
    @Column(name = "payment_link", length = 2048)
    private String paymentLink;

    // ========== Customer Details ==========

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_email", nullable = false, length = 255)
    private String customerEmail;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    // ========== Metadata & Error Handling ==========

    /**
     * Additional metadata in JSON format
     * Can store gateway-specific data, merchant data, etc.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Error message if payment failed
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * Gateway error code for debugging
     */
    @Column(name = "error_code", length = 50)
    private String errorCode;

    /**
     * Number of retry attempts for failed payments
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    // ========== Timestamps ==========

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Timestamp when payment was completed (SUCCESS/CAPTURED)
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Timestamp when payment link expires
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Timestamp of last retry attempt
     */
    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    // ========== Optimistic Locking ==========

    /**
     * Version for optimistic locking to prevent concurrent updates
     */
    @Version
    @Column(name = "version")
    private Long version;

    // ========== Business Methods ==========

    /**
     * Check if payment is in a terminal state (completed, cannot be changed)
     */
    public boolean isTerminal() {
        return status == PaymentStatus.SUCCESS ||
                status == PaymentStatus.CAPTURED ||
                status == PaymentStatus.FAILED ||       
                status == PaymentStatus.CANCELLED ||
                status == PaymentStatus.EXPIRED ||
                status == PaymentStatus.REFUNDED;
    }

    /**
     * Check if payment is successful
     */
    public boolean isSuccessful() {
        return status == PaymentStatus.SUCCESS ||
                status == PaymentStatus.CAPTURED;
    }

    /**
     * Check if payment link has expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if payment can be retried
     */
    public boolean canRetry() {
        return !isTerminal() && retryCount < 3;
    }
}
