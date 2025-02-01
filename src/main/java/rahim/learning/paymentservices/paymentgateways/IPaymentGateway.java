package rahim.learning.paymentservices.paymentgateways;

public interface IPaymentGateway {
    String createStandardPaymentLink(Long amount, String orderId, String phoneNumber, String name, String email);
}
