-- Create payments table
CREATE TABLE payments (
    id BINARY(16) PRIMARY KEY,
    order_id VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    gateway_type VARCHAR(20) NOT NULL,
    gateway_payment_id VARCHAR(255),
    payment_link VARCHAR(2048),
    
    -- Customer details
    customer_name VARCHAR(100) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    
    -- Metadata and error handling
    metadata TEXT,
    error_message VARCHAR(1000),
    error_code VARCHAR(50),
    retry_count INT NOT NULL DEFAULT 0,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    last_retry_at TIMESTAMP NULL,
    
    -- Optimistic locking
    version BIGINT NOT NULL DEFAULT 0,
    
    -- Indexes for performance
    INDEX idx_order_id (order_id),
    INDEX idx_payment_status (status),
    INDEX idx_gateway_payment_id (gateway_payment_id),
    INDEX idx_idempotency_key (idempotency_key),
    INDEX idx_created_at (created_at),
    INDEX idx_customer_email (customer_email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add comment to table
ALTER TABLE payments COMMENT = 'Stores payment transaction records with complete lifecycle tracking';
