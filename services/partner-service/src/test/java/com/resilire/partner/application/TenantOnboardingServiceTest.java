package com.resilire.partner.application;

import com.resilire.partner.application.TenantOnboardingService.OnboardingResult;
import com.resilire.partner.domain.ApiKeyStatus;
import com.resilire.partner.domain.TenantStatus;
import com.resilire.partner.infrastructure.persistence.TenantApiKeyJpaRepository;
import com.resilire.partner.infrastructure.persistence.TenantApiKeyRecord;
import com.resilire.partner.infrastructure.persistence.TenantJpaRepository;
import com.resilire.partner.infrastructure.persistence.TenantQuotaJpaRepository;
import com.resilire.partner.infrastructure.persistence.TenantQuotaRecord;
import com.resilire.partner.infrastructure.persistence.TenantRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantOnboardingServiceTest {

    private StubTenantRepo tenantRepo;
    private StubApiKeyRepo apiKeyRepo;
    private StubQuotaRepo quotaRepo;
    private TenantOnboardingService service;

    @BeforeEach
    void setUp() {
        tenantRepo = new StubTenantRepo();
        apiKeyRepo = new StubApiKeyRepo();
        quotaRepo = new StubQuotaRepo();

        TenantService tenantService = new TenantService(tenantRepo);
        ApiKeyService apiKeyService = new ApiKeyService(apiKeyRepo);
        QuotaService quotaService = new QuotaService(quotaRepo);

        service = new TenantOnboardingService(tenantService, apiKeyService, quotaService);
    }

    @Test
    void onboard_creates_tenant_key_and_quota_atomically() {
        OnboardingResult result = service.onboard("AcmeCorp", "ops@acme.com", "PROFESSIONAL");

        assertThat(result.tenantId()).isNotNull();
        assertThat(result.name()).isEqualTo("AcmeCorp");
        assertThat(result.rawApiKey()).startsWith("rsl_");
        assertThat(result.plan()).isEqualTo("PROFESSIONAL");
        assertThat(result.requestsPerMinute()).isEqualTo(100);
        assertThat(result.burstCapacity()).isEqualTo(150);
    }

    @Test
    void onboard_starter_plan_gets_correct_default_limits() {
        OnboardingResult result = service.onboard("SmallCo", "admin@small.co", "STARTER");

        assertThat(result.requestsPerMinute()).isEqualTo(20);
        assertThat(result.burstCapacity()).isEqualTo(30);
    }

    @Test
    void onboard_enterprise_plan_gets_high_limits() {
        OnboardingResult result = service.onboard("BigAirline", "tech@bigair.com", "ENTERPRISE");

        assertThat(result.requestsPerMinute()).isEqualTo(500);
        assertThat(result.burstCapacity()).isEqualTo(750);
    }

    @Test
    void raw_api_key_is_unique_per_onboarding() {
        OnboardingResult first = service.onboard("Corp1", "a@corp1.com", "STARTER");
        OnboardingResult second = service.onboard("Corp2", "a@corp2.com", "STARTER");

        assertThat(first.rawApiKey()).isNotEqualTo(second.rawApiKey());
    }

    @Test
    void duplicate_tenant_name_throws() {
        service.onboard("Unique", "a@unique.com", "STARTER");

        assertThatThrownBy(() -> service.onboard("Unique", "b@unique.com", "STARTER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unique");
    }

    // --- minimal stubs ---

    static class StubTenantRepo implements TenantJpaRepository {
        private final Map<String, TenantRecord> store = new HashMap<>();

        @Override public Optional<TenantRecord> findByName(String name) {
            return store.values().stream().filter(r -> r.getName().equals(name)).findFirst();
        }
        @Override public boolean existsByName(String name) {
            return store.values().stream().anyMatch(r -> r.getName().equals(name));
        }
        @Override public <S extends TenantRecord> S save(S r) { store.put(r.getId(), r); return r; }
        @Override public Optional<TenantRecord> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public <S extends TenantRecord> List<S> saveAll(Iterable<S> rs) {
            List<S> out = new ArrayList<>();
            rs.forEach(r -> { store.put(r.getId(), r); out.add(r); });
            return out;
        }
        @Override public boolean existsById(String id) { return store.containsKey(id); }
        @Override public List<TenantRecord> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<TenantRecord> findAllById(Iterable<String> ids) { return List.of(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(String id) { store.remove(id); }
        @Override public void delete(TenantRecord r) { store.remove(r.getId()); }
        @Override public void deleteAllById(Iterable<? extends String> ids) { ids.forEach(store::remove); }
        @Override public void deleteAll(Iterable<? extends TenantRecord> rs) { rs.forEach(r -> store.remove(r.getId())); }
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @SuppressWarnings("unchecked") @Override public <S extends TenantRecord> S saveAndFlush(S r) { return (S) save(r); }
        @Override public <S extends TenantRecord> List<S> saveAllAndFlush(Iterable<S> rs) { return saveAll(rs); }
        @Override public void deleteAllInBatch(Iterable<TenantRecord> rs) {}
        @Override public void deleteAllByIdInBatch(Iterable<String> ids) {}
        @Override public void deleteAllInBatch() { store.clear(); }
        @Override public TenantRecord getOne(String id) { return store.get(id); }
        @Override public TenantRecord getById(String id) { return store.get(id); }
        @Override public TenantRecord getReferenceById(String id) { return store.get(id); }
        @Override public <S extends TenantRecord> Optional<S> findOne(org.springframework.data.domain.Example<S> ex) { return Optional.empty(); }
        @Override public <S extends TenantRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex) { return List.of(); }
        @Override public <S extends TenantRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Sort s) { return List.of(); }
        @Override public <S extends TenantRecord> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends TenantRecord> long count(org.springframework.data.domain.Example<S> ex) { return 0; }
        @Override public <S extends TenantRecord> boolean exists(org.springframework.data.domain.Example<S> ex) { return false; }
        @Override public <S extends TenantRecord, R> R findBy(org.springframework.data.domain.Example<S> ex, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> q) { return null; }
        @Override public org.springframework.data.domain.Page<TenantRecord> findAll(org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public List<TenantRecord> findAll(org.springframework.data.domain.Sort s) { return new ArrayList<>(store.values()); }
    }

    static class StubApiKeyRepo implements TenantApiKeyJpaRepository {
        private final Map<String, TenantApiKeyRecord> store = new HashMap<>();

        @Override public List<TenantApiKeyRecord> findByTenantId(String t) {
            return store.values().stream().filter(r -> r.getTenantId().equals(t)).toList();
        }
        @Override public Optional<TenantApiKeyRecord> findByKeyHash(String h) {
            return store.values().stream().filter(r -> r.getKeyHash().equals(h)).findFirst();
        }
        @Override public <S extends TenantApiKeyRecord> S save(S r) { store.put(r.getId(), r); return r; }
        @Override public Optional<TenantApiKeyRecord> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public <S extends TenantApiKeyRecord> List<S> saveAll(Iterable<S> rs) {
            List<S> out = new ArrayList<>();
            rs.forEach(r -> { store.put(r.getId(), r); out.add(r); });
            return out;
        }
        @Override public boolean existsById(String id) { return store.containsKey(id); }
        @Override public List<TenantApiKeyRecord> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<TenantApiKeyRecord> findAllById(Iterable<String> ids) { return List.of(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(String id) { store.remove(id); }
        @Override public void delete(TenantApiKeyRecord r) { store.remove(r.getId()); }
        @Override public void deleteAllById(Iterable<? extends String> ids) { ids.forEach(store::remove); }
        @Override public void deleteAll(Iterable<? extends TenantApiKeyRecord> rs) { rs.forEach(r -> store.remove(r.getId())); }
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @SuppressWarnings("unchecked") @Override public <S extends TenantApiKeyRecord> S saveAndFlush(S r) { return (S) save(r); }
        @Override public <S extends TenantApiKeyRecord> List<S> saveAllAndFlush(Iterable<S> rs) { return saveAll(rs); }
        @Override public void deleteAllInBatch(Iterable<TenantApiKeyRecord> rs) {}
        @Override public void deleteAllByIdInBatch(Iterable<String> ids) {}
        @Override public void deleteAllInBatch() { store.clear(); }
        @Override public TenantApiKeyRecord getOne(String id) { return store.get(id); }
        @Override public TenantApiKeyRecord getById(String id) { return store.get(id); }
        @Override public TenantApiKeyRecord getReferenceById(String id) { return store.get(id); }
        @Override public <S extends TenantApiKeyRecord> Optional<S> findOne(org.springframework.data.domain.Example<S> ex) { return Optional.empty(); }
        @Override public <S extends TenantApiKeyRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex) { return List.of(); }
        @Override public <S extends TenantApiKeyRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Sort s) { return List.of(); }
        @Override public <S extends TenantApiKeyRecord> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends TenantApiKeyRecord> long count(org.springframework.data.domain.Example<S> ex) { return 0; }
        @Override public <S extends TenantApiKeyRecord> boolean exists(org.springframework.data.domain.Example<S> ex) { return false; }
        @Override public <S extends TenantApiKeyRecord, R> R findBy(org.springframework.data.domain.Example<S> ex, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> q) { return null; }
        @Override public org.springframework.data.domain.Page<TenantApiKeyRecord> findAll(org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public List<TenantApiKeyRecord> findAll(org.springframework.data.domain.Sort s) { return new ArrayList<>(store.values()); }
    }

    static class StubQuotaRepo implements TenantQuotaJpaRepository {
        private final Map<String, TenantQuotaRecord> store = new HashMap<>();

        @Override public Optional<TenantQuotaRecord> findByTenantId(String t) {
            return store.values().stream().filter(r -> r.getTenantId().equals(t)).findFirst();
        }
        @Override public <S extends TenantQuotaRecord> S save(S r) { store.put(r.getId(), r); return r; }
        @Override public Optional<TenantQuotaRecord> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public <S extends TenantQuotaRecord> List<S> saveAll(Iterable<S> rs) {
            List<S> out = new ArrayList<>();
            rs.forEach(r -> { store.put(r.getId(), r); out.add(r); });
            return out;
        }
        @Override public boolean existsById(String id) { return store.containsKey(id); }
        @Override public List<TenantQuotaRecord> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<TenantQuotaRecord> findAllById(Iterable<String> ids) { return List.of(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(String id) { store.remove(id); }
        @Override public void delete(TenantQuotaRecord r) { store.remove(r.getId()); }
        @Override public void deleteAllById(Iterable<? extends String> ids) { ids.forEach(store::remove); }
        @Override public void deleteAll(Iterable<? extends TenantQuotaRecord> rs) { rs.forEach(r -> store.remove(r.getId())); }
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @SuppressWarnings("unchecked") @Override public <S extends TenantQuotaRecord> S saveAndFlush(S r) { return (S) save(r); }
        @Override public <S extends TenantQuotaRecord> List<S> saveAllAndFlush(Iterable<S> rs) { return saveAll(rs); }
        @Override public void deleteAllInBatch(Iterable<TenantQuotaRecord> rs) {}
        @Override public void deleteAllByIdInBatch(Iterable<String> ids) {}
        @Override public void deleteAllInBatch() { store.clear(); }
        @Override public TenantQuotaRecord getOne(String id) { return store.get(id); }
        @Override public TenantQuotaRecord getById(String id) { return store.get(id); }
        @Override public TenantQuotaRecord getReferenceById(String id) { return store.get(id); }
        @Override public <S extends TenantQuotaRecord> Optional<S> findOne(org.springframework.data.domain.Example<S> ex) { return Optional.empty(); }
        @Override public <S extends TenantQuotaRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex) { return List.of(); }
        @Override public <S extends TenantQuotaRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Sort s) { return List.of(); }
        @Override public <S extends TenantQuotaRecord> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends TenantQuotaRecord> long count(org.springframework.data.domain.Example<S> ex) { return 0; }
        @Override public <S extends TenantQuotaRecord> boolean exists(org.springframework.data.domain.Example<S> ex) { return false; }
        @Override public <S extends TenantQuotaRecord, R> R findBy(org.springframework.data.domain.Example<S> ex, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> q) { return null; }
        @Override public org.springframework.data.domain.Page<TenantQuotaRecord> findAll(org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public List<TenantQuotaRecord> findAll(org.springframework.data.domain.Sort s) { return new ArrayList<>(store.values()); }
    }
}
