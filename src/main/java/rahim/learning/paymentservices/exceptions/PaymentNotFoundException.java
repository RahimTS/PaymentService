package rahim.learning.paymentservices.exceptions;

/**
 * Exception thrown when a payment record is not found in the database.
 */
public class PaymentNotFoundException extends PaymentException {
    
    public PaymentNotFoundException(String message) {
        super("PAYMENT_NOT_FOUND", message);
    }
    
    public PaymentNotFoundException(String paymentId, String field) {
        super("PAYMENT_NOT_FOUND", 
              String.format("Payment not found with %s: %s", field, paymentId));
    }
}
