package rahim.learning.paymentservices.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PaymentDto {
    Long amount;
    String orderId;
    String phoneNumber;
    String name;
    String email;
}
