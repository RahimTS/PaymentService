package rahim.learning.paymentservices.exceptions;

/**
 * Exception thrown when payment processing fails at the gateway level.
 * Can be retried based on the error type.
 */
public class PaymentProcessingException extends PaymentException {
    
    private final boolean retryable;
    
    public PaymentProcessingException(String message) {
        super("PAYMENT_PROCESSING_ERROR", message);
        this.retryable = false;
    }
    
    public PaymentProcessingException(String message, Throwable cause) {
        super("PAYMENT_PROCESSING_ERROR", message, cause);
        this.retryable = true; // Gateway errors are typically retryable
    }
    
    public PaymentProcessingException(String message, boolean retryable) {
        super("PAYMENT_PROCESSING_ERROR", message);
        this.retryable = retryable;
    }
    
    public boolean isRetryable() {
        return retryable;
    }
}
