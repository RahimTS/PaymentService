package rahim.learning.paymentservices.exceptions;

/**
 * Exception thrown when attempting to create a duplicate payment.
 * This is triggered by idempotency key violations.
 */
public class DuplicatePaymentException extends PaymentException {
    
    private final String existingPaymentId;
    
    public DuplicatePaymentException(String message, String existingPaymentId) {
        super("DUPLICATE_PAYMENT", message);
        this.existingPaymentId = existingPaymentId;
    }
    
    public String getExistingPaymentId() {
        return existingPaymentId;
    }
}
