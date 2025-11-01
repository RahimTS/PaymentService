package rahim.learning.paymentservices.metrics;

import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rahim.learning.paymentservices.entities.PaymentGatewayType;
import rahim.learning.paymentservices.entities.PaymentStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom Micrometer metrics for payment operations.
 * 
 * Provides business-specific metrics for monitoring and alerting:
 * - Payment success/failure rates
 * - Transaction volumes and amounts
 * - Gateway performance
 * - Processing latencies
 */
@Component
@Slf4j
public class PaymentMetrics {
    
    private final Counter paymentsCreatedCounter;
    private final Counter paymentsSuccessCounter;
    private final Counter paymentsFailedCounter;
    private final Timer paymentProcessingTimer;
    private final DistributionSummary paymentAmountSummary;
    private final AtomicInteger activePaymentsGauge;
    
    private final MeterRegistry registry;
    
    public PaymentMetrics(MeterRegistry registry) {
        this.registry = registry;
        
        // Counter: Total payments created
        this.paymentsCreatedCounter = Counter.builder("payments.created.total")
            .description("Total number of payments created")
            .tag("type", "creation")
            .register(registry);
        
        // Counter: Successful payments
        this.paymentsSuccessCounter = Counter.builder("payments.success.total")
            .description("Total number of successful payments")
            .tag("type", "success")
            .register(registry);
        
        // Counter: Failed payments
        this.paymentsFailedCounter = Counter.builder("payments.failed.total")
            .description("Total number of failed payments")
            .tag("type", "failure")
            .register(registry);
        
        // Timer: Payment processing duration
        this.paymentProcessingTimer = Timer.builder("payments.processing.duration")
            .description("Time taken to process payment")
            .publishPercentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram()
            .register(registry);
        
        // Distribution Summary: Payment amounts
        this.paymentAmountSummary = DistributionSummary.builder("payments.amount")
            .description("Distribution of payment amounts")
            .baseUnit("currency_units")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
        
        // Gauge: Active payments in processing
        this.activePaymentsGauge = registry.gauge(
            "payments.active",
            Tags.of("status", "processing"),
            new AtomicInteger(0)
        );
        
        log.info("Payment metrics initialized");
    }
    
    /**
     * Record payment creation
     */
    public void recordPaymentCreated(PaymentGatewayType gateway, BigDecimal amount) {
        paymentsCreatedCounter.increment();
        paymentAmountSummary.record(amount.doubleValue());
        
        // Gateway-specific counter
        Counter.builder("payments.created.by_gateway")
            .tag("gateway", gateway.toString())
            .register(registry)
            .increment();
        
        log.debug("Recorded payment creation: gateway={}, amount={}", gateway, amount);
    }
    
    /**
     * Record successful payment
     */
    public void recordPaymentSuccess(
        PaymentGatewayType gateway, 
        BigDecimal amount,
        Duration processingTime
    ) {
        paymentsSuccessCounter.increment();
        paymentProcessingTimer.record(processingTime);
        
        // Success rate by gateway
        Counter.builder("payments.success.by_gateway")
            .tag("gateway", gateway.toString())
            .register(registry)
            .increment();
        
        // Amount by gateway
        DistributionSummary.builder("payments.amount.by_gateway")
            .tag("gateway", gateway.toString())
            .register(registry)
            .record(amount.doubleValue());
        
        log.debug("Recorded successful payment: gateway={}, amount={}, duration={}ms",
                 gateway, amount, processingTime.toMillis());
    }
    
    /**
     * Record failed payment
     */
    public void recordPaymentFailure(
        PaymentGatewayType gateway,
        String errorCode,
        String errorReason
    ) {
        paymentsFailedCounter.increment();
        
        // Failure by gateway
        Counter.builder("payments.failed.by_gateway")
            .tag("gateway", gateway.toString())
            .tag("error_code", errorCode != null ? errorCode : "unknown")
            .register(registry)
            .increment();
        
        log.warn("Recorded payment failure: gateway={}, error={}", gateway, errorReason);
    }
    
    /**
     * Record payment retry
     */
    public void recordPaymentRetry(PaymentGatewayType gateway, int retryAttempt) {
        Counter.builder("payments.retry.total")
            .tag("gateway", gateway.toString())
            .tag("attempt", String.valueOf(retryAttempt))
            .register(registry)
            .increment();
        
        log.info("Recorded payment retry: gateway={}, attempt={}", gateway, retryAttempt);
    }
    
    /**
     * Record payment status change
     */
    public void recordStatusChange(PaymentStatus from, PaymentStatus to) {
        Counter.builder("payments.status.changes")
            .tag("from", from.toString())
            .tag("to", to.toString())
            .register(registry)
            .increment();
    }
    
    /**
     * Increment active payments
     */
    public void incrementActivePayments() {
        activePaymentsGauge.incrementAndGet();
    }
    
    /**
     * Decrement active payments
     */
    public void decrementActivePayments() {
        activePaymentsGauge.decrementAndGet();
    }
    
    /**
     * Record idempotency cache hit
     */
    public void recordIdempotencyCacheHit() {
        Counter.builder("payments.idempotency.cache_hit")
            .description("Number of idempotency cache hits")
            .register(registry)
            .increment();
    }
    
    /**
     * Record idempotency cache miss
     */
    public void recordIdempotencyCacheMiss() {
        Counter.builder("payments.idempotency.cache_miss")
            .description("Number of idempotency cache misses")
            .register(registry)
            .increment();
    }
}
