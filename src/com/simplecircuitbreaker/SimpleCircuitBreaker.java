package com.simplecircuitbreaker;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class SimpleCircuitBreaker {

    // States defined in the requirements
    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final int failureThreshold;      // N
    private final long failureWindowMillis;  // T (converted to millis)
    private final long cooldownMillis;       // Cooldown period

    private State state = State.CLOSED;
    private final Deque<Long> failureTimestamps = new LinkedList<>();
    private long lastFailureTime = 0;

    // Constructor
    public SimpleCircuitBreaker(int failureThreshold, long failureWindowTime, TimeUnit windowUnit, long cooldownTime, TimeUnit cooldownUnit) {
        this.failureThreshold = failureThreshold;
        this.failureWindowMillis = windowUnit.toMillis(failureWindowTime);
        this.cooldownMillis = cooldownUnit.toMillis(cooldownTime);
    }

    /**
     * Checks if a request is allowed to proceed.
     * Handles state transitions based on time (OPEN -> HALF_OPEN).
     */
    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();

        if (state == State.OPEN) {
            // Check if cooldown period has passed to switch to HALF-OPEN
            if (now - lastFailureTime >= cooldownMillis) {
                state = State.HALF_OPEN;
                System.out.println("State switched to HALF-OPEN (Cooldown finished)");
                return true; // Allow one test request
            }
            return false; // Still in cooldown, block request
        }

        // CLOSED or HALF-OPEN (if we are here in HALF-OPEN, we allow the probe)
        return true;
    }

    /**
     * Records a successful request.
     * Resets the breaker if it was in HALF-OPEN state.
     */
    public synchronized void recordSuccess() {
        if (state == State.HALF_OPEN) {
            state = State.CLOSED;
            failureTimestamps.clear(); // Reset failure history
            System.out.println("State switched to CLOSED (Test request succeeded)");
        }
        // If already CLOSED, do nothing
    }

    /**
     * Records a failed request.
     * Updates the rolling window and trips to OPEN if threshold is met.
     */
    public synchronized void recordFailure() {
        long now = System.currentTimeMillis();
        lastFailureTime = now;

        if (state == State.HALF_OPEN) {
            state = State.OPEN; // Test request failed, go back to OPEN
            System.out.println("State switched to OPEN (Test request failed)");
            return;
        }

        // If CLOSED, update rolling window
        failureTimestamps.addLast(now);
        cleanUpOldFailures(now);

        // Check if we hit the threshold (N)
        if (failureTimestamps.size() > failureThreshold) {
            state = State.OPEN;
            System.out.println("State switched to OPEN (Threshold exceeded: " + failureTimestamps.size() + " failures)");
        }
    }

    /**
     * Helper to remove failures that are outside the rolling window (T).
     */
    private void cleanUpOldFailures(long now) {
        while (!failureTimestamps.isEmpty() && (now - failureTimestamps.peekFirst() > failureWindowMillis)) {
            failureTimestamps.removeFirst();
        }
    }

    // Getter for testing purposes
    public synchronized State getState() {
        return state;
    }
}
