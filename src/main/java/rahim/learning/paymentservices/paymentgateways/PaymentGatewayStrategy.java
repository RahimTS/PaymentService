package rahim.learning.paymentservices.paymentgateways;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayStrategy {

    @Autowired
    private RazorPaymentGateway razorPaymentGateway;

    public IPaymentGateway getPaymentGateway() {
        return razorPaymentGateway;
    }
}
