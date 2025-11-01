package rahim.learning.paymentservices.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Service for managing idempotency keys using Redis.
 * Prevents duplicate payment processing.
 */
@Service
@Slf4j
public class IdempotencyService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);
    
    /**
     * Check if idempotency key has been processed
     */
    public boolean isProcessed(String idempotencyKey) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        try {
            Boolean exists = redisTemplate.hasKey(key);
            log.debug("Idempotency check for key {}: {}", idempotencyKey, exists);
            return Boolean.TRUE.equals(exists);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable during idempotency check; falling back to DB. cause={}", ex.getMessage());
            return false;
        } catch (Exception ex) {
            log.warn("Idempotency check error; falling back to DB. cause={}", ex.getMessage());
            return false;
        }
    }
    
    /**
     * Mark idempotency key as processed
     */
    public void markAsProcessed(String idempotencyKey, UUID paymentId) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        try {
            redisTemplate.opsForValue().set(key, paymentId.toString(), TTL);
            log.info("Marked idempotency key {} as processed for payment {}", idempotencyKey, paymentId);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable when marking idempotency; continuing without cache. cause={}", ex.getMessage());
        } catch (Exception ex) {
            log.warn("Failed to mark idempotency; continuing without cache. cause={}", ex.getMessage());
        }
    }
    
    /**
     * Get payment ID for idempotency key
     */
    public String getPaymentId(String idempotencyKey) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable during getPaymentId; returning null. cause={}", ex.getMessage());
            return null;
        } catch (Exception ex) {
            log.warn("Error reading idempotency from Redis; returning null. cause={}", ex.getMessage());
            return null;
        }
    }
    
    /**
     * Remove idempotency key (for testing or expiration)
     */
    public void remove(String idempotencyKey) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        try {
            redisTemplate.delete(key);
            log.info("Removed idempotency key: {}", idempotencyKey);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable when removing idempotency; ignoring. cause={}", ex.getMessage());
        } catch (Exception ex) {
            log.warn("Failed to remove idempotency key; ignoring. cause={}", ex.getMessage());
        }
    }
}
