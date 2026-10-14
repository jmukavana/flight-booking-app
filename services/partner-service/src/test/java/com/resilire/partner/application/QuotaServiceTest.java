package com.resilire.partner.application;

import com.resilire.partner.infrastructure.persistence.TenantQuotaJpaRepository;
import com.resilire.partner.infrastructure.persistence.TenantQuotaRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuotaServiceTest {

    private QuotaService service;

    @BeforeEach
    void setUp() {
        service = new QuotaService(new StubQuotaRepo());
    }

    @Test
    void starter_plan_gets_default_limits() {
        TenantQuotaRecord r = service.getOrCreate("t1", "STARTER");
        assertThat(r.getRequestsPerMinute()).isEqualTo(20);
        assertThat(r.getBurstCapacity()).isEqualTo(30);
        assertThat(r.getPlan()).isEqualTo("STARTER");
    }

    @Test
    void enterprise_plan_gets_higher_limits() {
        TenantQuotaRecord r = service.getOrCreate("t2", "ENTERPRISE");
        assertThat(r.getRequestsPerMinute()).isEqualTo(500);
        assertThat(r.getBurstCapacity()).isEqualTo(750);
    }

    @Test
    void get_or_create_is_idempotent() {
        TenantQuotaRecord first = service.getOrCreate("t3", "PROFESSIONAL");
        TenantQuotaRecord second = service.getOrCreate("t3", "PROFESSIONAL");
        assertThat(first.getId()).isEqualTo(second.getId());
    }

    @Test
    void update_quota_changes_limits() {
        service.getOrCreate("t4", "STARTER");
        TenantQuotaRecord updated = service.updateQuota("t4", "PROFESSIONAL", 80, 120);
        assertThat(updated.getRequestsPerMinute()).isEqualTo(80);
        assertThat(updated.getBurstCapacity()).isEqualTo(120);
        assertThat(updated.getPlan()).isEqualTo("PROFESSIONAL");
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    static class StubQuotaRepo implements TenantQuotaJpaRepository {
        private final Map<String, TenantQuotaRecord> store = new HashMap<>();

        @Override
        public Optional<TenantQuotaRecord> findByTenantId(String tenantId) {
            return store.values().stream().filter(r -> r.getTenantId().equals(tenantId)).findFirst();
        }

        @Override
        public <S extends TenantQuotaRecord> S save(S r) {
            store.put(r.getId(), r);
            return r;
        }

        @Override public <S extends TenantQuotaRecord> List<S> saveAll(Iterable<S> rs) {
            List<S> out = new ArrayList<>();
            rs.forEach(r -> { store.put(r.getId(), r); out.add(r); });
            return out;
        }
        @Override public Optional<TenantQuotaRecord> findById(String id) { return Optional.ofNullable(store.get(id)); }
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
        @SuppressWarnings("unchecked")
        @Override public <S extends TenantQuotaRecord> S saveAndFlush(S r) { return (S) save(r); }
        @Override public <S extends TenantQuotaRecord> List<S> saveAllAndFlush(Iterable<S> rs) { return saveAll(rs); }
        @Override public void deleteAllInBatch(Iterable<TenantQuotaRecord> rs) {}
        @Override public void deleteAllByIdInBatch(Iterable<String> ids) {}
        @Override public void deleteAllInBatch() { store.clear(); }
        @Override public TenantQuotaRecord getOne(String id) { return store.get(id); }
        @Override public TenantQuotaRecord getById(String id) { return store.get(id); }
        @Override public TenantQuotaRecord getReferenceById(String id) { return store.get(id); }
        @Override public <S extends TenantQuotaRecord> Optional<S> findOne(org.springframework.data.domain.Example<S> ex) { return Optional.empty(); }
        @Override public <S extends TenantQuotaRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex) { return List.of(); }
        @Override public <S extends TenantQuotaRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public <S extends TenantQuotaRecord> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends TenantQuotaRecord> long count(org.springframework.data.domain.Example<S> ex) { return 0; }
        @Override public <S extends TenantQuotaRecord> boolean exists(org.springframework.data.domain.Example<S> ex) { return false; }
        @Override public <S extends TenantQuotaRecord, R> R findBy(org.springframework.data.domain.Example<S> ex, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> q) { return null; }
        @Override public org.springframework.data.domain.Page<TenantQuotaRecord> findAll(org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public List<TenantQuotaRecord> findAll(org.springframework.data.domain.Sort sort) { return new ArrayList<>(store.values()); }
    }
}
