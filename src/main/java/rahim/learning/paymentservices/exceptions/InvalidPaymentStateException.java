package rahim.learning.paymentservices.exceptions;

/**
 * Exception thrown when attempting an invalid state transition.
 * For example, trying to capture an already captured payment.
 */
public class InvalidPaymentStateException extends PaymentException {
    
    private final String currentState;
    private final String attemptedAction;
    
    public InvalidPaymentStateException(String currentState, String attemptedAction) {
        super("INVALID_PAYMENT_STATE", 
              String.format("Cannot perform '%s' on payment in '%s' state", 
                          attemptedAction, currentState));
        this.currentState = currentState;
        this.attemptedAction = attemptedAction;
    }
    
    public String getCurrentState() {
        return currentState;
    }
    
    public String getAttemptedAction() {
        return attemptedAction;
    }
}
