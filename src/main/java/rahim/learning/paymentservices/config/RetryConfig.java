package rahim.learning.paymentservices.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Enable Spring Retry for payment operations.
 * Used in PaymentService for gateway call retries.
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // Configuration is handled via @Retryable annotations in service methods
}
