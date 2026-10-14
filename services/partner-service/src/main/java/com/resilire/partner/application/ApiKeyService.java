package com.resilire.partner.application;

import com.resilire.partner.domain.ApiKeyStatus;
import com.resilire.partner.infrastructure.persistence.TenantApiKeyJpaRepository;
import com.resilire.partner.infrastructure.persistence.TenantApiKeyRecord;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApiKeyService {

    private final TenantApiKeyJpaRepository keys;
    private final SecureRandom random = new SecureRandom();

    public ApiKeyService(TenantApiKeyJpaRepository keys) {
        this.keys = keys;
    }

    /** Issues a new API key. Returns the raw key — caller must store it; it won't be retrievable again. */
    public IssuedKey issueKey(String tenantId, String description) {
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String rawKey = "rsl_" + Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        String hash = sha256Hex(rawKey);

        TenantApiKeyRecord record = new TenantApiKeyRecord();
        record.setId(UUID.randomUUID().toString());
        record.setTenantId(tenantId);
        record.setKeyHash(hash);
        record.setStatus(ApiKeyStatus.ACTIVE);
        record.setDescription(description);
        record.setCreatedAt(Instant.now());
        keys.save(record);

        return new IssuedKey(record.getId(), rawKey);
    }

    public TenantApiKeyRecord revokeKey(String keyId) {
        TenantApiKeyRecord record = keys.findById(keyId)
                .orElseThrow(() -> new NoSuchElementException("API key not found: " + keyId));
        record.setStatus(ApiKeyStatus.REVOKED);
        record.setRevokedAt(Instant.now());
        return keys.save(record);
    }

    public List<TenantApiKeyRecord> findByTenantId(String tenantId) {
        return keys.findByTenantId(tenantId);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record IssuedKey(String keyId, String rawKey) {}
}
