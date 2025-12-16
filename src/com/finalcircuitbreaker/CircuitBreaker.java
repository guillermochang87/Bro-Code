package com.finalcircuitbreaker;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class CircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);

    private final int failureThreshold;
    private final long cooldownMillis;

    public CircuitBreaker(int failureThreshold, long cooldown, TimeUnit unit) {
        this.failureThreshold = failureThreshold;
        this.cooldownMillis = unit.toMillis(cooldown);
    }

    public <T> T execute(Callable<T> action) throws Exception {
        if (!allowRequest()) {
            throw new CallNotPermittedException("CircuitBreaker is OPEN or HALF_OPEN (probe in progress)");
        }

        try {
            T result = action.call();
            onSuccess();
            return result;
        } catch (Exception e) {
            // We re-throw CallNotPermittedException immediately, otherwise we record failure
            if (!(e instanceof CallNotPermittedException)) {
                onFailure();
            }
            throw e;
        }
    }

    private boolean allowRequest() {
        State currentState = state.get();

        if (currentState == State.CLOSED) {
            return true;
        }

        if (currentState == State.OPEN) {
            long now = System.currentTimeMillis();
            if (now - lastFailureTime.get() > cooldownMillis) {
                return state.compareAndSet(State.OPEN, State.HALF_OPEN);
            }
            return false;
        }
        return false;
    }

    private void onSuccess() {
        if (state.get() == State.HALF_OPEN) {
            if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                failureCount.set(0);
            }
        } else {
            failureCount.set(0); 
        }
    }

    private void onFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        if (state.get() == State.HALF_OPEN) {
            state.set(State.OPEN);
            return;
        }
        int failures = failureCount.incrementAndGet();
        if (failures >= failureThreshold) {
            state.set(State.OPEN);
        }
    }

    public State getState() {
        return state.get();
    }

    public static class CallNotPermittedException extends RuntimeException {
        public CallNotPermittedException(String message) { super(message); }
    }
    // --- MAIN METHOD FOR TESTING ---
    public static void main(String[] args) throws InterruptedException {
        CircuitBreaker breaker = new CircuitBreaker(3, 2, TimeUnit.SECONDS);

        System.out.println("--- 1. START: EVERYTHING WORKING ---");
        attemptRequest(breaker, false);
        attemptRequest(breaker, false);
        printState(breaker);

        System.out.println("\n--- 2. SIMULATING FAILURES (Threshold 3) ---");
        attemptRequest(breaker, true);
        attemptRequest(breaker, true);
        attemptRequest(breaker, true); // Opens here
        
        printState(breaker);

        System.out.println("\n--- 3. IMMEDIATE RETRY (Should be blocked) ---");
        attemptRequest(breaker, false);

        System.out.println("\n--- 4. WAITING FOR COOLDOWN (2.5s) ---");
        Thread.sleep(2500);

        System.out.println("\n--- 5. RECOVERY PROBE (Half-Open) ---");
        // This request triggers the atomic switch OPEN -> HALF_OPEN
        // If it succeeds, it switches HALF_OPEN -> CLOSED
        attemptRequest(breaker, false);
        printState(breaker);
    }

    private static void attemptRequest(CircuitBreaker breaker, boolean forceFailure) {
        try {
            String response = breaker.execute(() -> callExternalApi(forceFailure));
            System.out.println("SUCCESS: " + response);
        } catch (CallNotPermittedException e) {
             System.err.println("BLOCKED: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("REMOTE ERROR: " + e.getMessage());
        }
    }
    private static String callExternalApi(boolean triggerFailure) {
        if (triggerFailure) throw new RuntimeException("500 Error");
        return "Data Payload";
    }
    private static void printState(CircuitBreaker breaker) {
        System.out.println("[STATE]: " + breaker.getState());
    }
}