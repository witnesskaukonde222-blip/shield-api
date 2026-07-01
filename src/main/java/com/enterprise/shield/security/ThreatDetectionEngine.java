package com.enterprise.shield.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ThreatDetectionEngine {

    private final StringRedisTemplate redisTemplate;
    private static final int THRESHOLD_LIMIT = 5;
    private static final long TIME_WINDOW_MINUTES = 1;
    private static final long BLOCK_DURATION_HOURS = 24;

    public ThreatDetectionEngine(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Returns true if the given client key (IP address or username) should
     * be diverted to the honeypot decoy instead of the real business layer.
     */
    public boolean isThreatDetected(String clientKey) {
        String key = "threat:counter:" + clientKey;
        String blockKey = "threat:blocked:" + clientKey;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(blockKey))) {
            return true;
        }

        Long currentCount = redisTemplate.opsForValue().increment(key);
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(TIME_WINDOW_MINUTES));
        }

        if (currentCount != null && currentCount >= THRESHOLD_LIMIT) {
            redisTemplate.opsForValue().set(blockKey, "ACTIVE_DECOY_FLAG", Duration.ofHours(BLOCK_DURATION_HOURS));
            return true;
        }

        return false;
    }

    /** Manually clears a block - exposed for admin remediation endpoints. */
    public void clearBlock(String clientKey) {
        redisTemplate.delete("threat:blocked:" + clientKey);
        redisTemplate.delete("threat:counter:" + clientKey);
    }
}
