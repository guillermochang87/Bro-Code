package com.simplecircuitbreaker;

import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        // CONFIGURATION: 
        // Opens after 3 consecutive failures.
        // Waits 2 seconds (Cooldown) before trying again.
        CircuitBreaker breaker = new CircuitBreaker(3, 2, TimeUnit.SECONDS);

        System.out.println("--- 1. START: EVERYTHING WORKING ---");
        attemptRequest(breaker, false); // Success
        attemptRequest(breaker, false); // Success
        printState(breaker);

        System.out.println("\n--- 2. SIMULATING FAILURES (Threshold is 3) ---");
        attemptRequest(breaker, true); // Failure 1
        attemptRequest(breaker, true); // Failure 2
        attemptRequest(breaker, true); // Failure 3 -> CIRCUIT SHOULD OPEN HERE
        
        printState(breaker); // Should be OPEN

        System.out.println("\n--- 3. IMMEDIATE RETRY (Should be blocked locally) ---");
        attemptRequest(breaker, false); // Even if the API is fine, the breaker blocks it

        System.out.println("\n--- 4. WAITING FOR COOLDOWN (2.5 seconds) ---");
        Thread.sleep(2500); 

        System.out.println("\n--- 5. RECOVERY ATTEMPT (Half-Open) ---");
        // Time has passed. This request is allowed through. If it succeeds, the breaker closes.
        attemptRequest(breaker, false); 
        
        printState(breaker); // Should be back to CLOSED
    }

    // Helper method to execute requests
    private static void attemptRequest(CircuitBreaker breaker, boolean forceFailure) {
        try {
            String response = breaker.execute(() -> callExternalApi(forceFailure));
            System.out.println("✅ SUCCESS: " + response);
        } catch (Exception e) {
            System.err.println("❌ CONTROLLED ERROR: " + e.getMessage());
        }
    }

    // Simulates an external service (Database, API, etc.)
    private static String callExternalApi(boolean triggerFailure) {
        if (triggerFailure) {
            throw new RuntimeException("Simulated 500 Connection Error");
        }
        return "Data received successfully";
    }

    private static void printState(CircuitBreaker breaker) {
        System.out.println("[CURRENT BREAKER STATE]: " + breaker.getState());
    }
}