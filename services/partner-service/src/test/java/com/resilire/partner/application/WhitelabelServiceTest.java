package com.resilire.partner.application;

import com.resilire.partner.infrastructure.persistence.WhitelabelConfigJpaRepository;
import com.resilire.partner.infrastructure.persistence.WhitelabelConfigRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WhitelabelServiceTest {

    private WhitelabelService service;

    @BeforeEach
    void setUp() {
        service = new WhitelabelService(new StubWhitelabelRepo());
    }

    @Test
    void upsert_creates_new_config_for_tenant() {
        WhitelabelConfigRecord r = service.upsertConfig("t1", "https://cdn/logo.png", "#123456", "app.partner.com");
        assertThat(r.getTenantId()).isEqualTo("t1");
        assertThat(r.getLogoUrl()).isEqualTo("https://cdn/logo.png");
        assertThat(r.getPrimaryColor()).isEqualTo("#123456");
        assertThat(r.getCustomDomain()).isEqualTo("app.partner.com");
        assertThat(r.getId()).isNotNull();
        assertThat(r.getCreatedAt()).isNotNull();
    }

    @Test
    void upsert_updates_existing_config() {
        service.upsertConfig("t2", "https://old/logo.png", "#000000", "old.domain.com");
        WhitelabelConfigRecord updated = service.upsertConfig("t2", "https://new/logo.png", "#ffffff", "new.domain.com");
        assertThat(updated.getLogoUrl()).isEqualTo("https://new/logo.png");
        assertThat(updated.getPrimaryColor()).isEqualTo("#ffffff");
        assertThat(updated.getCustomDomain()).isEqualTo("new.domain.com");
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    void find_by_tenant_id_returns_empty_when_not_configured() {
        assertThat(service.findByTenantId("unknown")).isEmpty();
    }

    @Test
    void find_by_tenant_id_returns_config_after_upsert() {
        service.upsertConfig("t3", null, "#aabbcc", null);
        Optional<WhitelabelConfigRecord> found = service.findByTenantId("t3");
        assertThat(found).isPresent();
        assertThat(found.get().getPrimaryColor()).isEqualTo("#aabbcc");
    }

    static class StubWhitelabelRepo implements WhitelabelConfigJpaRepository {
        private final Map<String, WhitelabelConfigRecord> store = new HashMap<>();

        @Override
        public Optional<WhitelabelConfigRecord> findByTenantId(String tenantId) {
            return store.values().stream().filter(r -> r.getTenantId().equals(tenantId)).findFirst();
        }

        @Override
        public <S extends WhitelabelConfigRecord> S save(S r) {
            store.put(r.getId(), r);
            return r;
        }

        @Override public <S extends WhitelabelConfigRecord> List<S> saveAll(Iterable<S> rs) {
            List<S> out = new ArrayList<>();
            rs.forEach(r -> { store.put(r.getId(), r); out.add(r); });
            return out;
        }
        @Override public Optional<WhitelabelConfigRecord> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public boolean existsById(String id) { return store.containsKey(id); }
        @Override public List<WhitelabelConfigRecord> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<WhitelabelConfigRecord> findAllById(Iterable<String> ids) { return List.of(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(String id) { store.remove(id); }
        @Override public void delete(WhitelabelConfigRecord r) { store.remove(r.getId()); }
        @Override public void deleteAllById(Iterable<? extends String> ids) { ids.forEach(store::remove); }
        @Override public void deleteAll(Iterable<? extends WhitelabelConfigRecord> rs) { rs.forEach(r -> store.remove(r.getId())); }
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @SuppressWarnings("unchecked")
        @Override public <S extends WhitelabelConfigRecord> S saveAndFlush(S r) { return (S) save(r); }
        @Override public <S extends WhitelabelConfigRecord> List<S> saveAllAndFlush(Iterable<S> rs) { return saveAll(rs); }
        @Override public void deleteAllInBatch(Iterable<WhitelabelConfigRecord> rs) {}
        @Override public void deleteAllByIdInBatch(Iterable<String> ids) {}
        @Override public void deleteAllInBatch() { store.clear(); }
        @Override public WhitelabelConfigRecord getOne(String id) { return store.get(id); }
        @Override public WhitelabelConfigRecord getById(String id) { return store.get(id); }
        @Override public WhitelabelConfigRecord getReferenceById(String id) { return store.get(id); }
        @Override public <S extends WhitelabelConfigRecord> Optional<S> findOne(org.springframework.data.domain.Example<S> ex) { return Optional.empty(); }
        @Override public <S extends WhitelabelConfigRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex) { return List.of(); }
        @Override public <S extends WhitelabelConfigRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public <S extends WhitelabelConfigRecord> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends WhitelabelConfigRecord> long count(org.springframework.data.domain.Example<S> ex) { return 0; }
        @Override public <S extends WhitelabelConfigRecord> boolean exists(org.springframework.data.domain.Example<S> ex) { return false; }
        @Override public <S extends WhitelabelConfigRecord, R> R findBy(org.springframework.data.domain.Example<S> ex, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> q) { return null; }
        @Override public org.springframework.data.domain.Page<WhitelabelConfigRecord> findAll(org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public List<WhitelabelConfigRecord> findAll(org.springframework.data.domain.Sort sort) { return new ArrayList<>(store.values()); }
    }
}
