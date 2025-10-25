package rahim.learning.paymentservices.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PaymentEvent represents an immutable record of payment state changes.
 * Implements event sourcing pattern for complete audit trail.
 * 
 * Every state change in payment lifecycle is recorded as an event.
 * Events are never updated or deleted, only appended.
 */
@Entity
@Table(name = "payment_events", indexes = {
        @Index(name = "idx_payment_id", columnList = "payment_id"),
        @Index(name = "idx_event_type", columnList = "event_type"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Reference to the payment this event belongs to
     */
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    /**
     * Type of event (e.g., PAYMENT_INITIATED, PAYMENT_LINK_CREATED,
     * PAYMENT_SUCCESS)
     */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /**
     * Event payload in JSON format
     * Contains event-specific data
     */
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    /**
     * Additional metadata about the event
     * Can contain user agent, IP address, source system, etc.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Source of the event (e.g., API, WEBHOOK, SCHEDULED_JOB)
     */
    @Column(name = "source", length = 50)
    private String source;

    /**
     * User/system that triggered this event
     */
    @Column(name = "triggered_by", length = 100)
    private String triggeredBy;

    /**
     * Timestamp when event was created (immutable)
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Sequence number for ordering events
     * Helps in event replay and debugging
     */
    @Column(name = "sequence_number")
    private Long sequenceNumber;
}
