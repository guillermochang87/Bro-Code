package com.circuitbreaker;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TokenBucketRateLimiter {
    private final int capacity;
    private final double refillRate; // tokens per millisecond
    private double tokens;
    private long lastRefillTimestamp;
    private final ReentrantLock lock = new ReentrantLock();

    public TokenBucketRateLimiter(int capacity, int refillRate) {
        if (capacity <= 0 || refillRate <= 0) {
            throw new IllegalArgumentException("Capacity and refill rate must be positive");
        }
        
        this.capacity = capacity;
        this.refillRate = refillRate / 1000.0; // Convert to tokens per millisecond
        this.tokens = capacity;
        this.lastRefillTimestamp = System.currentTimeMillis();
    }

    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    public boolean tryAcquire(int tokensRequested) {
        if (tokensRequested <= 0 || tokensRequested > capacity) {
            throw new IllegalArgumentException("Invalid token request amount");
        }

        lock.lock();
        try {
            refillTokens();
            
            if (tokens >= tokensRequested) {
                tokens -= tokensRequested;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public boolean tryAcquire(int tokensRequested, long timeout, TimeUnit unit) {
        if (tokensRequested <= 0 || tokensRequested > capacity) {
            throw new IllegalArgumentException("Invalid token request amount");
        }

        long timeoutMs = unit.toMillis(timeout);
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (tryAcquire(tokensRequested)) {
                return true;
            }
            
            try {
                // Sleep briefly to avoid busy waiting
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return false;
    }

    private void refillTokens() {
        long currentTime = System.currentTimeMillis();
        long timeElapsed = currentTime - lastRefillTimestamp;
        
        if (timeElapsed > 0) {
            double tokensToAdd = timeElapsed * refillRate;
            tokens = Math.min(capacity, tokens + tokensToAdd);
            lastRefillTimestamp = currentTime;
        }
    }

    public int getAvailableTokens() {
        lock.lock();
        try {
            refillTokens();
            return (int) Math.floor(tokens);
        } finally {
            lock.unlock();
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public double getRefillRate() {
        return refillRate * 1000; // Convert back to tokens per second
    }
}
