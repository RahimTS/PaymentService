package rahim.learning.paymentservices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class PaymentservicesApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentservicesApplication.class, args);
	}

}
