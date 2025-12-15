package com.circuitbreaker;

public class ExampleUsage {
    public static void main(String[] args) {
        // Circuit Breaker Example
        CircuitBreaker circuitBreaker = new CircuitBreaker(5, 10, 30); // 5 failures in 10 seconds, 30s cooldown
        
        // Token Bucket Example
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(10, 5); // 10 capacity, 5 tokens/sec
        
        // Simulated service call with both circuit breaker and rate limiting
        for (int i = 0; i < 20; i++) {
            if (circuitBreaker.allowRequest() && rateLimiter.tryAcquire()) {
                try {
                    // Make your service call here
                    boolean success = makeServiceCall();
                    
                    if (success) {
                        circuitBreaker.recordSuccess();
                    } else {
                        circuitBreaker.recordFailure();
                    }
                } catch (Exception e) {
                    circuitBreaker.recordFailure();
                }
            } else {
                System.out.println("Request blocked - Circuit: " + circuitBreaker.getState() + 
                                 ", Rate limit: " + (rateLimiter.getAvailableTokens() > 0 ? "available" : "exceeded"));
            }
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private static boolean makeServiceCall() {
        // Simulate service call
        return Math.random() > 0.3;
    }
}
