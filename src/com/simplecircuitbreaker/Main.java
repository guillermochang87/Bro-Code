package com.simplecircuitbreaker;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Setup: Trip if > 2 failures in 10 seconds. Cooldown is 3 seconds.
        SimpleCircuitBreaker breaker = new SimpleCircuitBreaker(2, 10, TimeUnit.SECONDS, 3, TimeUnit.SECONDS);

        // 1. Normal State
        System.out.println("Request 1 Allowed? " + breaker.allowRequest()); // True
        breaker.recordFailure(); // Failure 1
        
        System.out.println("Request 2 Allowed? " + breaker.allowRequest()); // True
        breaker.recordFailure(); // Failure 2 (Still <= N)
        
        System.out.println("Request 3 Allowed? " + breaker.allowRequest()); // True
        breaker.recordFailure(); // Failure 3 (> N, Trips to OPEN)

        // 2. Open State (Blocking)
        System.out.println("Request 4 (Immediate) Allowed? " + breaker.allowRequest()); // False

        // 3. Wait for Cooldown
        System.out.println("... Waiting for cooldown (3s) ...");
        Thread.sleep(3500);

        // 4. Half-Open State
        // The next call checks time, sees cooldown passed, switches to HALF-OPEN
        boolean testProbe = breaker.allowRequest();
        System.out.println("Probe Request Allowed? " + testProbe); // True

        // 5. Reset to Closed
        if (testProbe) {
            // Simulate success on the probe
            breaker.recordSuccess(); 
        }

        System.out.println("Final State: " + breaker.getState()); // CLOSED
    }
}