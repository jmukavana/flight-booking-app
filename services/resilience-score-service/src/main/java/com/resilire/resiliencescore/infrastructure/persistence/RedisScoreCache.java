package com.resilire.resiliencescore.infrastructure.persistence;

import com.resilire.resiliencescore.domain.score.ItinerarySignature;
import com.resilire.resiliencescore.domain.score.ResilienceScore;
import com.resilire.resiliencescore.domain.score.ScoreCache;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Redis adapter for the score cache. Stores each score as JSON under
 * {@code score:<signature>} with a TTL, so entries self-expire and a stale score
 * is eventually recomputed. A corrupt entry is treated as a miss rather than
 * failing the request.
 */
@Component
class RedisScoreCache implements ScoreCache {

    private static final Logger log = LoggerFactory.getLogger(RedisScoreCache.class);
    private static final String KEY_PREFIX = "score:";

    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final Duration ttl;

    RedisScoreCache(StringRedisTemplate redis, ObjectMapper json,
                    @Value("${resilire.score.cache-ttl-seconds:3600}") long ttlSeconds) {
        this.redis = redis;
        this.json = json;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    @Override
    public Optional<ResilienceScore> find(ItinerarySignature signature) {
        String raw = redis.opsForValue().get(key(signature));
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(json.readValue(raw, ResilienceScore.class));
        } catch (JacksonException e) {
            log.warn("Discarding unreadable cached score for {}: {}", signature.value(), e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(ItinerarySignature signature, ResilienceScore score) {
        redis.opsForValue().set(key(signature), serialize(score), ttl);
    }

    private String serialize(ResilienceScore score) {
        try {
            return json.writeValueAsString(score);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to serialize score", e);
        }
    }

    private String key(ItinerarySignature signature) {
        return KEY_PREFIX + signature.value();
    }
}
