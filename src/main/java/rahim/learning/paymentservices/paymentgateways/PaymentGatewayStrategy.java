package rahim.learning.paymentservices.paymentgateways;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayStrategy {

    @Autowired
    private RazorPaymentGateway razorPaymentGateway;

    @Autowired
    private StripePaymentGateway stripePaymentGateway;

    public IPaymentGateway getPaymentGateway() {
        return stripePaymentGateway;
    }
}
