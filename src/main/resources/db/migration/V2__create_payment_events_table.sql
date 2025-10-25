-- Create payment_events table for event sourcing
CREATE TABLE payment_events (
    id BINARY(16) PRIMARY KEY,
    payment_id BINARY(16) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT,
    metadata TEXT,
    source VARCHAR(50),
    triggered_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sequence_number BIGINT,
    
    -- Indexes
    INDEX idx_payment_id (payment_id),
    INDEX idx_event_type (event_type),
    INDEX idx_created_at (created_at),
    
    -- Foreign key constraint
    CONSTRAINT fk_payment_events_payment_id 
        FOREIGN KEY (payment_id) 
        REFERENCES payments(id) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add comment
ALTER TABLE payment_events COMMENT = 'Event sourcing table for payment lifecycle audit trail';
