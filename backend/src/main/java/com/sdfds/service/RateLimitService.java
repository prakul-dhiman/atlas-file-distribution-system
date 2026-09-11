package com.sdfds.service;

public interface RateLimitService {

    /**
     * Checks whether the given key is allowed to proceed.
     * @param key unique identifier (e.g. IP + endpoint)
     * @param maxAttempts maximum allowed attempts in the window
     * @param windowSeconds the sliding/fixed window duration in seconds
     * @return true if allowed, false if rate limit exceeded
     */
    boolean isAllowed(String key, int maxAttempts, long windowSeconds);
}