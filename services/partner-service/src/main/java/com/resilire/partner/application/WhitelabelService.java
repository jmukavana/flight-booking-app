package com.resilire.partner.application;

import com.resilire.partner.infrastructure.persistence.WhitelabelConfigJpaRepository;
import com.resilire.partner.infrastructure.persistence.WhitelabelConfigRecord;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhitelabelService {

    private final WhitelabelConfigJpaRepository configs;

    public WhitelabelService(WhitelabelConfigJpaRepository configs) {
        this.configs = configs;
    }

    @Transactional
    public WhitelabelConfigRecord upsertConfig(String tenantId, String logoUrl,
                                               String primaryColor, String customDomain) {
        WhitelabelConfigRecord record = configs.findByTenantId(tenantId)
                .orElseGet(() -> {
                    WhitelabelConfigRecord r = new WhitelabelConfigRecord();
                    r.setId(UUID.randomUUID().toString());
                    r.setTenantId(tenantId);
                    r.setCreatedAt(Instant.now());
                    return r;
                });
        record.setLogoUrl(logoUrl);
        record.setPrimaryColor(primaryColor);
        record.setCustomDomain(customDomain);
        record.setUpdatedAt(Instant.now());
        return configs.save(record);
    }

    @Transactional(readOnly = true)
    public Optional<WhitelabelConfigRecord> findByTenantId(String tenantId) {
        return configs.findByTenantId(tenantId);
    }
}
