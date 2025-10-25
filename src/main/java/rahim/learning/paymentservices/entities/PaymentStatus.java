package rahim.learning.paymentservices.entities;

/**
 * Represents the lifecycle states of a payment transaction.
 * Follows industry-standard payment processing workflow.
 */
public enum PaymentStatus {
    /**
     * Payment record created but not yet sent to gateway
     */
    INITIATED,

    /**
     * Payment link generated, awaiting customer action
     */
    PENDING,

    /**
     * Payment being processed by gateway
     */
    PROCESSING,

    /**
     * Payment authorized but not yet captured (for two-step payments)
     */
    AUTHORIZED,

    /**
     * Payment amount captured from authorized payment
     */
    CAPTURED,

    /**
     * Payment successfully completed
     */
    SUCCESS,

    /**
     * Payment failed (insufficient funds, card declined, etc.)
     */
    FAILED,

    /**
     * Payment link expired without completion
     */
    EXPIRED,

    /**
     * Payment cancelled by user or merchant
     */
    CANCELLED,

    /**
     * Payment fully refunded
     */
    REFUNDED,

    /**
     * Payment partially refunded
     */
    PARTIALLY_REFUNDED
}
