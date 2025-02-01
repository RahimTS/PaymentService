package rahim.learning.paymentservices.controllers;

import com.razorpay.Payment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rahim.learning.paymentservices.dtos.PaymentDto;
import rahim.learning.paymentservices.services.IPaymentService;
import rahim.learning.paymentservices.services.PaymentService;

@RestController
@RequestMapping("api/v1/payment")
public class PaymentController {

    @Autowired
    private IPaymentService paymentService;

    @PostMapping
    public String payment(@RequestBody PaymentDto payment) {
        return paymentService.getPaymentLink(payment.getAmount(), payment.getOrderId(), payment.getPhoneNumber(), payment.getName(), payment.getEmail());
    }
}
