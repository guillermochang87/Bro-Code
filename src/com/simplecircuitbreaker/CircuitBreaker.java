package com.simplecircuitbreaker;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private State state = State.CLOSED;
    private int failureCount = 0;
    private long lastFailureTime = 0;
    
    private final int failureThreshold;
    private final long cooldownMillis;

    public CircuitBreaker(int failureThreshold, long cooldown, TimeUnit unit) {
        this.failureThreshold = failureThreshold;
        this.cooldownMillis = unit.toMillis(cooldown);
    }

    // This is the main entry point. Wraps the call in a try-catch block automatically.
    public synchronized <T> T execute(Supplier<T> action) throws Exception {
        checkState();
        try {
            T result = action.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e; // Re-throw so the caller knows something went wrong
        }
    }

    private void checkState() {
        if (state == State.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime > cooldownMillis) {
                state = State.HALF_OPEN; // Cooldown finished, allow one probe request
            } else {
                throw new RuntimeException("CircuitBreaker is OPEN, request blocked.");
            }
        }
    }

    private void onSuccess() {
        if (state == State.HALF_OPEN) {
            state = State.CLOSED;
            failureCount = 0;
        }
        // Always reset failure count on success to keep a clean slate
        failureCount = 0; 
    }

    private void onFailure() {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();
        
        if (state == State.HALF_OPEN || failureCount >= failureThreshold) {
            state = State.OPEN;
        }
    }
    
    public synchronized State getState() { return state; }
}