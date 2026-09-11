package com.sdfds.service.impl;

import com.sdfds.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitServiceImpl.class);
    private static final String KEY_PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean isAllowed(String key, int maxAttempts, long windowSeconds) {
        String redisKey = KEY_PREFIX + key;
        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                // First request in the window — set expiry
                redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
            }
            boolean allowed = count != null && count <= maxAttempts;
            if (!allowed) {
                log.warn("Rate limit exceeded for key [{}] (count={}, max={})", key, count, maxAttempts);
            }
            return allowed;
        } catch (Exception e) {
            // If Redis is unavailable, fail open to avoid breaking auth flows
            log.error("Rate limiter unavailable, allowing request: {}", e.getMessage());
            return true;
        }
    }
}