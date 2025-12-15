package com.simplecircuitbreaker;

import java.util.concurrent.TimeUnit;

public class CircuitBreakerTest {

    public static void main(String[] args) throws InterruptedException {
        // CONFIGURATION:
        // Opens if there are more than 2 failures (threshold)
        // In a 10-second window
        // 3-second wait time (cooldown) to retry
        SimpleCircuitBreaker breaker = new SimpleCircuitBreaker(
            2,                  // Failure threshold
            10, TimeUnit.SECONDS, // Time window
            3, TimeUnit.SECONDS   // Cooldown
        );

        // 1. Simulate successful requests (CLOSED State)
        System.out.println("--- Start (CLOSED) ---");
        performRequest(breaker, true); // Success
        performRequest(breaker, true); // Success

        // 2. Simulate failures to open the circuit
        System.out.println("\n--- Triggering failures ---");
        performRequest(breaker, false); // Failure 1
        performRequest(breaker, false); // Failure 2
        performRequest(breaker, false); // Failure 3 (Should open here)

        // 3. Attempt request while OPEN (Should be blocked immediately)
        System.out.println("\n--- Attempting during OPEN (Immediate block) ---");
        performRequest(breaker, true); 

        // 4. Wait for Cooldown (3 seconds)
        System.out.println("\n--- Waiting 4 seconds (Cooldown) ---");
        Thread.sleep(4000);

        // 5. Test request (HALF-OPEN)
        System.out.println("\n--- Retry (HALF-OPEN) ---");
        // If this succeeds, the circuit closes. If it fails, it opens again.
        performRequest(breaker, true); 

        // 6. Verify return to normal
        System.out.println("\n--- Verifying final state ---");
        System.out.println("Current state: " + breaker.getState());
    }

    // Helper method to simulate a request
    private static void performRequest(SimpleCircuitBreaker breaker, boolean shouldSucceed) {
        if (breaker.allowRequest()) {
            try {
                // Simulate business logic (DB call, API, etc.)
                if (!shouldSucceed) {
                    throw new RuntimeException("Simulated error");
                }
                System.out.println("✅ Request processed successfully.");
                breaker.recordSuccess();
            } catch (Exception e) {
                System.out.println("❌ Request failed: " + e.getMessage());
                breaker.recordFailure();
            }
        } else {
            System.out.println("⛔ Circuit Breaker OPEN. Request blocked.");
        }
    }
}