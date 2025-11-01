package rahim.learning.paymentservices.schedulers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rahim.learning.paymentservices.entities.Payment;
import rahim.learning.paymentservices.entities.PaymentStatus;
import rahim.learning.paymentservices.repositories.PaymentRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled jobs for payment maintenance
 */
@Component
@Slf4j
public class PaymentScheduler {

    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Mark expired payments - runs every 15 minutes
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void markExpiredPayments() {
        log.info("Starting expired payment marking job");

        try {
            LocalDateTime now = LocalDateTime.now();
            List<Payment> expiredPayments = paymentRepository.findExpiredPayments(now);

            if (expiredPayments.isEmpty()) {
                log.debug("No expired payments found");
                return;
            }

            int count = 0;
            for (Payment payment : expiredPayments) {
                payment.setStatus(PaymentStatus.EXPIRED);
                paymentRepository.save(payment);
                count++;
            }

            log.info("Marked {} payments as EXPIRED", count);

        } catch (Exception e) {
            log.error("Error marking expired payments", e);
        }
    }

    /**
     * Generate daily metrics - runs at midnight
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void generateDailyMetrics() {
        log.info("Generating daily payment metrics");

        try {
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            LocalDateTime today = LocalDateTime.now();

            long totalPayments = paymentRepository
                    .findByCreatedAtBetween(yesterday, today)
                    .size();

            long successfulPayments = paymentRepository
                    .findByCreatedAtBetween(yesterday, today)
                    .stream()
                    .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                    .count();

            double successRate = totalPayments > 0
                    ? (double) successfulPayments / totalPayments * 100
                    : 0;

            log.info("Daily Metrics - Total: {}, Success: {}, Rate: {:.2f}%",
                    totalPayments, successfulPayments, successRate);

        } catch (Exception e) {
            log.error("Error generating daily metrics", e);
        }
    }

    /**
     * Retry failed payments - runs hourly
     */
    @Scheduled(cron = "0 0 * * * *")
    public void retryFailedPayments() {
        log.info("Starting failed payment retry job");

        try {
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            List<Payment> retryablePayments = paymentRepository
                    .findRetryablePayments(PaymentStatus.FAILED, 3, since);

            if (retryablePayments.isEmpty()) {
                log.debug("No failed payments eligible for retry");
                return;
            }

            log.info("Found {} payments eligible for retry", retryablePayments.size());

        } catch (Exception e) {
            log.error("Error processing retry job", e);
        }
    }
}
