package com.circuitbreaker;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class CircuitBreaker {
    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final ConcurrentHashMap<Long, Boolean> failureWindow = new ConcurrentHashMap<>();
    
    private final int failureThreshold;
    private final long windowSizeMs;
    private final long cooldownPeriodMs;
    private volatile long openTime = 0;
    private final ReentrantLock lock = new ReentrantLock();

    public CircuitBreaker(int failureThreshold, long windowSizeSeconds, long cooldownPeriodSeconds) {
        this.failureThreshold = failureThreshold;
        this.windowSizeMs = windowSizeSeconds * 1000;
        this.cooldownPeriodMs = cooldownPeriodSeconds * 1000;
    }

    public boolean allowRequest() {
        State currentState = state.get();
        
        if (currentState == State.OPEN) {
            // Check if cooldown period has elapsed
            if (System.currentTimeMillis() - openTime >= cooldownPeriodMs) {
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    System.out.println("Circuit breaker transitioning to HALF_OPEN");
                    return true;
                }
            }
            return false;
        }
        
        return true;
    }

    public void recordSuccess() {
        lock.lock();
        try {
            if (state.get() == State.HALF_OPEN) {
                reset();
                System.out.println("Circuit breaker reset to CLOSED from HALF_OPEN");
            } else {
                cleanupOldFailures();
            }
        } finally {
            lock.unlock();
        }
    }

    public void recordFailure() {
        lock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            if (state.get() == State.HALF_OPEN) {
                // If failure in HALF_OPEN, go back to OPEN
                state.set(State.OPEN);
                openTime = currentTime;
                System.out.println("Circuit breaker back to OPEN from HALF_OPEN");
                return;
            }
            
            // Add failure to rolling window
            failureWindow.put(currentTime, true);
            cleanupOldFailures();
            
            // Check if we should trip to OPEN
            if (failureWindow.size() >= failureThreshold && state.compareAndSet(State.CLOSED, State.OPEN)) {
                openTime = currentTime;
                System.out.println("Circuit breaker tripped to OPEN");
            }
        } finally {
            lock.unlock();
        }
    }

    private void cleanupOldFailures() {
        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - windowSizeMs;
        
        failureWindow.keySet().removeIf(timestamp -> timestamp < cutoffTime);
        failureCount.set(failureWindow.size());
    }

    private void reset() {
        failureWindow.clear();
        failureCount.set(0);
        state.set(State.CLOSED);
    }

    public State getState() {
        return state.get();
    }

    public int getFailureCount() {
        return failureCount.get();
    }
}
