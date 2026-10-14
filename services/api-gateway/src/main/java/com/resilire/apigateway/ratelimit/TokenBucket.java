package com.resilire.apigateway.ratelimit;

/**
 * Thread-safe token bucket for per-tenant rate limiting. Tokens refill
 * continuously at {@code refillPerMinute / 60} per second up to {@code capacity}.
 * Each request consumes one token; a missing token signals throttle.
 */
public class TokenBucket {

    private final int capacity;
    private final double refillPerNano;
    private double tokens;
    private long lastNanos;

    public TokenBucket(int capacity, int refillPerMinute) {
        this.capacity = capacity;
        this.refillPerNano = (double) refillPerMinute / 60_000_000_000.0;
        this.tokens = capacity;
        this.lastNanos = System.nanoTime();
    }

    TokenBucket(int capacity, int refillPerMinute, long nowNanos) {
        this.capacity = capacity;
        this.refillPerNano = (double) refillPerMinute / 60_000_000_000.0;
        this.tokens = capacity;
        this.lastNanos = nowNanos;
    }

    public synchronized boolean tryConsume() {
        return tryConsume(System.nanoTime());
    }

    synchronized boolean tryConsume(long nowNanos) {
        double elapsed = nowNanos - lastNanos;
        tokens = Math.min(capacity, tokens + elapsed * refillPerNano);
        lastNanos = nowNanos;
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    public synchronized double availableTokens() {
        double elapsed = System.nanoTime() - lastNanos;
        return Math.min(capacity, tokens + elapsed * refillPerNano);
    }
}
