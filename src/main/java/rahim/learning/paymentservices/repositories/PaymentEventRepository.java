package rahim.learning.paymentservices.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rahim.learning.paymentservices.entities.PaymentEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for PaymentEvent entity.
 * Supports event sourcing and audit trail queries.
 */
@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {
    
    /**
     * Get all events for a payment in chronological order
     * Used for reconstructing payment state from events
     */
    List<PaymentEvent> findByPaymentIdOrderByCreatedAtAsc(UUID paymentId);
    
    /**
     * Get events by type
     */
    List<PaymentEvent> findByEventType(String eventType);
    
    /**
     * Get events for a payment within a time range
     */
    List<PaymentEvent> findByPaymentIdAndCreatedAtBetween(
        UUID paymentId,
        LocalDateTime startTime,
        LocalDateTime endTime
    );
    
    /**
     * Get latest N events for a payment
     */
    @Query("SELECT e FROM PaymentEvent e WHERE e.paymentId = :paymentId " +
           "ORDER BY e.createdAt DESC LIMIT :limit")
    List<PaymentEvent> findLatestEventsByPaymentId(
        @Param("paymentId") UUID paymentId,
        @Param("limit") int limit
    );
    
    /**
     * Count events by type for analytics
     */
    @Query("SELECT e.eventType, COUNT(e) FROM PaymentEvent e " +
           "WHERE e.createdAt >= :since GROUP BY e.eventType")
    List<Object[]> countByEventType(@Param("since") LocalDateTime since);
    
    /**
     * Find events by source (API, WEBHOOK, etc.)
     */
    List<PaymentEvent> findBySource(String source);
}
