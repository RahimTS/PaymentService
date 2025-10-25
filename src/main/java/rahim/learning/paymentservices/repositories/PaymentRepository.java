package rahim.learning.paymentservices.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rahim.learning.paymentservices.entities.Payment;
import rahim.learning.paymentservices.entities.PaymentGatewayType;
import rahim.learning.paymentservices.entities.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Payment entity.
 * Provides custom query methods for payment operations.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    // ========== Lookup Methods ==========
    
    /**
     * Find payment by idempotency key (for duplicate prevention)
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    
    /**
     * Find payment by external order ID
     */
    Optional<Payment> findByOrderId(String orderId);
    
    /**
     * Find payment by gateway payment ID (from webhook processing)
     */
    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);
    
    /**
     * Check if payment exists for given order ID
     */
    boolean existsByOrderId(String orderId);
    
    // ========== Status-based Queries ==========
    
    /**
     * Find all payments with specific status
     */
    List<Payment> findByStatus(PaymentStatus status);
    
    /**
     * Find payments by status and gateway type
     */
    List<Payment> findByStatusAndGatewayType(
        PaymentStatus status, 
        PaymentGatewayType gatewayType
    );
    
    /**
     * Find payments created before cutoff time with specific status
     * Useful for finding expired or stuck payments
     */
    List<Payment> findByStatusAndCreatedAtBefore(
        PaymentStatus status, 
        LocalDateTime cutoffTime
    );
    
    /**
     * Find expired payments that haven't been marked as expired
     */
    @Query("SELECT p FROM Payment p WHERE p.expiresAt < :now " +
           "AND p.status NOT IN ('EXPIRED', 'SUCCESS', 'CAPTURED', 'CANCELLED')")
    List<Payment> findExpiredPayments(@Param("now") LocalDateTime now);
    
    // ========== Retry Logic Queries ==========
    
    /**
     * Find payments eligible for retry
     * - Failed status
     * - Retry count less than max retries
     * - Created within retry window
     */
    @Query("SELECT p FROM Payment p WHERE p.status = :status " +
           "AND p.retryCount < :maxRetries " +
           "AND p.createdAt >= :since " +
           "ORDER BY p.createdAt ASC")
    List<Payment> findRetryablePayments(
        @Param("status") PaymentStatus status,
        @Param("maxRetries") Integer maxRetries,
        @Param("since") LocalDateTime since
    );
    
    // ========== Customer Queries ==========
    
    /**
     * Find all payments for a customer by email
     */
    List<Payment> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);
    
    /**
     * Find recent payments for a customer
     */
    @Query("SELECT p FROM Payment p WHERE p.customerEmail = :email " +
           "AND p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<Payment> findRecentPaymentsByCustomer(
        @Param("email") String email,
        @Param("since") LocalDateTime since
    );
    
    // ========== Metrics & Analytics Queries ==========
    
    /**
     * Count payments by status since a given time
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status " +
           "AND p.createdAt >= :since")
    Long countByStatusSince(
        @Param("status") PaymentStatus status,
        @Param("since") LocalDateTime since
    );
    
    /**
     * Sum of payment amounts by status since a given time
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.status = :status AND p.createdAt >= :since")
    BigDecimal sumAmountByStatusSince(
        @Param("status") PaymentStatus status,
        @Param("since") LocalDateTime since
    );
    
    /**
     * Get payment success rate for a time period
     */
    @Query("SELECT " +
           "CAST(COUNT(CASE WHEN p.status IN ('SUCCESS', 'CAPTURED') THEN 1 END) AS double) / " +
           "CAST(COUNT(p) AS double) * 100 " +
           "FROM Payment p WHERE p.createdAt >= :since")
    Double calculateSuccessRate(@Param("since") LocalDateTime since);
    
    /**
     * Get average payment amount for successful payments
     */
    @Query("SELECT AVG(p.amount) FROM Payment p " +
           "WHERE p.status IN ('SUCCESS', 'CAPTURED') " +
           "AND p.createdAt >= :since")
    BigDecimal getAverageSuccessfulPaymentAmount(@Param("since") LocalDateTime since);
    
    /**
     * Count payments by gateway type
     */
    @Query("SELECT p.gatewayType, COUNT(p) FROM Payment p " +
           "WHERE p.createdAt >= :since GROUP BY p.gatewayType")
    List<Object[]> countByGatewayType(@Param("since") LocalDateTime since);
    
    // ========== Time-based Queries ==========
    
    /**
     * Find payments created between two timestamps
     */
    List<Payment> findByCreatedAtBetween(
        LocalDateTime startTime, 
        LocalDateTime endTime
    );
    
    /**
     * Find payments completed in a time range
     */
    List<Payment> findByCompletedAtBetween(
        LocalDateTime startTime, 
        LocalDateTime endTime
    );
}
