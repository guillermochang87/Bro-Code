package com.tokenbucketratelimiter;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Capacity of 10 tokens, refills 1 token per second
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(10, 1);

        // Simulate a burst of 15 requests
        for (int i = 1; i <= 15; i++) {
            boolean allowed = limiter.allowRequest();
            System.out.println("Request " + i + ": " + (allowed ? "Allowed" : "Blocked"));
        }

        // Wait 2 seconds (should refill ~2 tokens)
        Thread.sleep(2000);
        System.out.println("Request after wait: " + (limiter.allowRequest() ? "Allowed" : "Blocked"));
    }
}
