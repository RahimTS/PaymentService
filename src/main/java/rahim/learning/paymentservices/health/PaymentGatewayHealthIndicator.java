package rahim.learning.paymentservices.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import rahim.learning.paymentservices.paymentgateways.PaymentGatewayStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Health indicator for payment gateways
 */
@Component
@Slf4j
public class PaymentGatewayHealthIndicator implements HealthIndicator {

    @Autowired
    private PaymentGatewayStrategy gatewayStrategy;

    @Override
    public Health health() {
        try {
            List<String> availableGateways = gatewayStrategy.getAvailableGateways();

            Map<String, Object> details = new HashMap<>();
            details.put("available_gateways", availableGateways);
            details.put("gateway_count", availableGateways.size());
            details.put("stripe_available", gatewayStrategy.isGatewayAvailable("STRIPE"));
            details.put("razorpay_available", gatewayStrategy.isGatewayAvailable("RAZORPAY"));

            if (availableGateways.isEmpty()) {
                return Health.down()
                        .withDetail("error", "No payment gateways available")
                        .withDetails(details)
                        .build();
            }

            return Health.up().withDetails(details).build();

        } catch (Exception e) {
            log.error("Error checking payment gateway health", e);
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}
