package com.resilire.apigateway.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBucketTest {

    @Test
    void full_bucket_allows_up_to_capacity_requests_immediately() {
        TokenBucket bucket = new TokenBucket(5, 60, 0L);

        int allowed = 0;
        for (int i = 0; i < 10; i++) {
            if (bucket.tryConsume(0L)) allowed++;
        }

        assertThat(allowed).isEqualTo(5);
    }

    @Test
    void exhausted_bucket_refills_after_one_minute() {
        long nowNanos = 0L;
        TokenBucket bucket = new TokenBucket(3, 60, nowNanos);

        bucket.tryConsume(nowNanos);
        bucket.tryConsume(nowNanos);
        bucket.tryConsume(nowNanos);
        assertThat(bucket.tryConsume(nowNanos)).isFalse();

        long oneMinuteNanos = 60_000_000_000L;
        assertThat(bucket.tryConsume(nowNanos + oneMinuteNanos)).isTrue();
    }

    @Test
    void partial_refill_grants_proportional_tokens() {
        long nowNanos = 0L;
        TokenBucket bucket = new TokenBucket(60, 60, nowNanos);
        for (int i = 0; i < 60; i++) bucket.tryConsume(nowNanos);

        long halfMinuteNanos = 30_000_000_000L;
        int allowed = 0;
        for (int i = 0; i < 40; i++) {
            if (bucket.tryConsume(nowNanos + halfMinuteNanos)) allowed++;
            else break;
        }

        assertThat(allowed).isBetween(29, 31);
    }

    @Test
    void tokens_do_not_exceed_capacity() {
        long nowNanos = 0L;
        TokenBucket bucket = new TokenBucket(10, 60, nowNanos);

        long twoMinutesNanos = 120_000_000_000L;
        assertThat(bucket.availableTokens()).isLessThanOrEqualTo(10.0);
        int allowed = 0;
        for (int i = 0; i < 20; i++) {
            if (bucket.tryConsume(nowNanos + twoMinutesNanos)) allowed++;
        }
        assertThat(allowed).isEqualTo(10);
    }
}
