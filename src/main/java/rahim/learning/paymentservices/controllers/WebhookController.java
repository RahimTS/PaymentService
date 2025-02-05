package rahim.learning.paymentservices.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stripeWebhook")
public class WebhookController {

    @PostMapping
    public void webhook(@RequestBody String webhook) {
        System.out.println(webhook);
    }
}
