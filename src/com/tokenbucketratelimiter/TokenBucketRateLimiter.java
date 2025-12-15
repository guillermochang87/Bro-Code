package com.tokenbucketratelimiter;

import java.util.concurrent.TimeUnit;

public class TokenBucketRateLimiter {

    private final long capacity;
    private final double refillRatePerNanosecond;

    private double currentTokens;
    private long lastRefillTimestamp;

    /**
     * @param capacity   The maximum number of tokens the bucket can hold.
     * @param refillRate The number of new tokens added to the bucket per second.
     */
    public TokenBucketRateLimiter(int capacity, int refillRate) {
        this.capacity = capacity;
        // Convert rate from tokens/second to tokens/nanosecond for higher precision
        this.refillRatePerNanosecond = (double) refillRate / 1_000_000_000.0;
        
        // Start with a full bucket
        this.currentTokens = capacity;
        this.lastRefillTimestamp = System.nanoTime();
    }

    /**
     * Attempts to consume a single token for a request.
     * * @return true if the request is allowed (token consumed), false otherwise.
     */
    public synchronized boolean allowRequest() {
        refill();

        if (currentTokens >= 1) {
            currentTokens -= 1;
            return true;
        }
        
        return false;
    }

    /**
     * Refills the bucket based on time elapsed since the last check.
     * This follows the "Lazy Refill" strategy.
     */
    private void refill() {
        long now = System.nanoTime();
        long timeElapsed = now - lastRefillTimestamp;
        
        if (timeElapsed > 0) {
            // Calculate how many tokens to add based on elapsed time
            double tokensToAdd = timeElapsed * refillRatePerNanosecond;
            
            // Update tokens, ensuring we don't exceed capacity
            currentTokens = Math.min(capacity, currentTokens + tokensToAdd);
            
            // Update timestamp
            lastRefillTimestamp = now;
        }
    }
}
