package rahim.learning.paymentservices.services;

import org.springframework.stereotype.Service;

@Service
public class PaymentService implements IPaymentService {

    @Override
    public String getPaymentLink(Long amount, String orderId, String phoneNumber, String name, String email) {
        return "";
    }
}
