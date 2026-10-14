package com.resilire.apigateway.ratelimit;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-tenant token bucket registry. Buckets are created on first use with
 * service-wide defaults. Partners registered by the partner-service can override
 * their limits via {@link #register} (called from a periodic sync task).
 */
public class QuotaRegistry {

    private final int defaultRpm;
    private final int defaultBurst;
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, int[]> limits = new ConcurrentHashMap<>();

    public QuotaRegistry(int defaultRpm, int defaultBurst) {
        this.defaultRpm = defaultRpm;
        this.defaultBurst = defaultBurst;
    }

    /** Register explicit quota for an API key hash (synced from partner-service). */
    public void register(String apiKeyHash, int rpm, int burst) {
        limits.put(apiKeyHash, new int[]{rpm, burst});
        buckets.remove(apiKeyHash);
    }

    /**
     * Attempt to consume one request token for the given API key hash.
     * Returns {@code false} if the tenant is over quota (caller should 429).
     */
    public boolean tryConsume(String apiKeyHash) {
        TokenBucket bucket = buckets.computeIfAbsent(apiKeyHash, k -> {
            int[] lim = limits.get(k);
            int rpm = lim != null ? lim[0] : defaultRpm;
            int burst = lim != null ? lim[1] : defaultBurst;
            return new TokenBucket(burst, rpm);
        });
        return bucket.tryConsume();
    }

    public int registeredTenants() {
        return limits.size();
    }
}
