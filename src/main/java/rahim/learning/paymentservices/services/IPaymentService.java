package rahim.learning.paymentservices.services;

public interface IPaymentService {
    String getPaymentLink(Long amount, String orderId, String phoneNumber, String name, String email);
}
