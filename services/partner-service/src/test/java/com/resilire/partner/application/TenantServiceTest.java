package com.resilire.partner.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resilire.partner.domain.ApiKeyStatus;
import com.resilire.partner.domain.TenantStatus;
import com.resilire.partner.infrastructure.persistence.TenantApiKeyJpaRepository;
import com.resilire.partner.infrastructure.persistence.TenantApiKeyRecord;
import com.resilire.partner.infrastructure.persistence.TenantJpaRepository;
import com.resilire.partner.infrastructure.persistence.TenantRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.function.Function;

class TenantServiceTest {

    private InMemoryTenantRepo tenantRepo;
    private InMemoryKeyRepo keyRepo;
    private TenantService tenantService;
    private ApiKeyService keyService;

    @BeforeEach
    void setUp() {
        tenantRepo = new InMemoryTenantRepo();
        keyRepo = new InMemoryKeyRepo();
        tenantService = new TenantService(tenantRepo);
        keyService = new ApiKeyService(keyRepo);
    }

    @Test
    void create_tenant_persists_with_active_status() {
        TenantRecord t = tenantService.createTenant("Kenya Airways", "ops@kq.co.ke", "PROFESSIONAL");

        assertThat(t.getName()).isEqualTo("Kenya Airways");
        assertThat(t.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(t.getCreatedAt()).isNotNull();
    }

    @Test
    void duplicate_name_throws_illegal_argument() {
        tenantService.createTenant("Resilire Demo", "demo@resilire.io", "STARTER");

        assertThatThrownBy(() -> tenantService.createTenant("Resilire Demo", "other@resilire.io", "STARTER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Resilire Demo");
    }

    @Test
    void suspend_tenant_transitions_to_suspended() {
        TenantRecord t = tenantService.createTenant("RwandAir", "ops@rwandair.com", "ENTERPRISE");

        TenantRecord suspended = tenantService.suspendTenant(t.getId());

        assertThat(suspended.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
    }

    @Test
    void activate_suspended_tenant_transitions_to_active() {
        TenantRecord t = tenantService.createTenant("Fastjet", "ops@fastjet.com", "STARTER");
        tenantService.suspendTenant(t.getId());

        TenantRecord activated = tenantService.activateTenant(t.getId());

        assertThat(activated.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    void issue_api_key_stores_hash_not_raw_key() {
        TenantRecord t = tenantService.createTenant("TravelPort", "dev@travelport.com", "PROFESSIONAL");
        ApiKeyService.IssuedKey issued = keyService.issueKey(t.getId(), "CI test key");

        assertThat(issued.rawKey()).startsWith("rsl_");
        TenantApiKeyRecord stored = keyRepo.store.get(issued.keyId());
        assertThat(stored).isNotNull();
        assertThat(stored.getKeyHash()).doesNotContain(issued.rawKey());
        assertThat(stored.getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);
    }

    @Test
    void revoke_api_key_marks_revoked() {
        TenantRecord t = tenantService.createTenant("Sabre Corp", "api@sabre.com", "ENTERPRISE");
        ApiKeyService.IssuedKey issued = keyService.issueKey(t.getId(), "prod key");

        TenantApiKeyRecord revoked = keyService.revokeKey(issued.keyId());

        assertThat(revoked.getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
        assertThat(revoked.getRevokedAt()).isNotNull();
    }

    // --- in-memory stubs ---

    private static class InMemoryTenantRepo implements TenantJpaRepository {
        final Map<String, TenantRecord> store = new HashMap<>();
        public TenantRecord save(TenantRecord r) { store.put(r.getId(), r); return r; }
        public Optional<TenantRecord> findByName(String name) {
            return store.values().stream().filter(r -> name.equals(r.getName())).findFirst();
        }
        public boolean existsByName(String name) {
            return store.values().stream().anyMatch(r -> name.equals(r.getName()));
        }
        public Optional<TenantRecord> findById(String id) { return Optional.ofNullable(store.get(id)); }
        public List<TenantRecord> findAll() { return new ArrayList<>(store.values()); }
        public boolean existsById(String id) { return store.containsKey(id); }
        public void deleteById(String id) { store.remove(id); }
        public void delete(TenantRecord r) { store.remove(r.getId()); }
        public long count() { return store.size(); }
        public <S extends TenantRecord> List<S> saveAll(Iterable<S> rs) { List<S> out = new ArrayList<>(); rs.forEach(r -> { store.put(r.getId(), r); out.add(r); }); return out; }
        public List<TenantRecord> findAllById(Iterable<String> ids) { return List.of(); }
        public void deleteAll() { store.clear(); }
        public void deleteAll(Iterable<? extends TenantRecord> rs) {}
        public void deleteAllById(Iterable<? extends String> ids) {}
        public void flush() {}
        @SuppressWarnings("unchecked") public <S extends TenantRecord> S saveAndFlush(S r) { return (S) save(r); }
        public <S extends TenantRecord> List<S> saveAllAndFlush(Iterable<S> rs) { return saveAll(rs); }
        public void deleteAllInBatch() {}
        public void deleteAllInBatch(Iterable<TenantRecord> rs) {}
        public void deleteAllByIdInBatch(Iterable<String> ids) {}
        public TenantRecord getOne(String id) { return store.get(id); }
        public TenantRecord getById(String id) { return store.get(id); }
        public TenantRecord getReferenceById(String id) { return store.get(id); }
        public <S extends TenantRecord> Optional<S> findOne(Example<S> ex) { return Optional.empty(); }
        public <S extends TenantRecord> List<S> findAll(Example<S> ex) { return List.of(); }
        public <S extends TenantRecord> List<S> findAll(Example<S> ex, Sort sort) { return List.of(); }
        public <S extends TenantRecord> Page<S> findAll(Example<S> ex, Pageable p) { return Page.empty(); }
        public <S extends TenantRecord> long count(Example<S> ex) { return 0; }
        public <S extends TenantRecord> boolean exists(Example<S> ex) { return false; }
        public <S extends TenantRecord, R> R findBy(Example<S> ex, Function<FluentQuery.FetchableFluentQuery<S>, R> fn) { return null; }
        public List<TenantRecord> findAll(Sort sort) { return findAll(); }
        public Page<TenantRecord> findAll(Pageable p) { return Page.empty(); }
    }

    private static class InMemoryKeyRepo implements TenantApiKeyJpaRepository {
        final Map<String, TenantApiKeyRecord> store = new HashMap<>();
        public TenantApiKeyRecord save(TenantApiKeyRecord r) { store.put(r.getId(), r); return r; }
        public List<TenantApiKeyRecord> findByTenantId(String id) {
            return store.values().stream().filter(r -> id.equals(r.getTenantId())).toList();
        }
        public Optional<TenantApiKeyRecord> findByKeyHash(String hash) {
            return store.values().stream().filter(r -> hash.equals(r.getKeyHash())).findFirst();
        }
        public Optional<TenantApiKeyRecord> findById(String id) { return Optional.ofNullable(store.get(id)); }
        public List<TenantApiKeyRecord> findAll() { return new ArrayList<>(store.values()); }
        public boolean existsById(String id) { return store.containsKey(id); }
        public void deleteById(String id) { store.remove(id); }
        public void delete(TenantApiKeyRecord r) { store.remove(r.getId()); }
        public long count() { return store.size(); }
        public <S extends TenantApiKeyRecord> List<S> saveAll(Iterable<S> rs) { List<S> out = new ArrayList<>(); rs.forEach(r -> { store.put(r.getId(), r); out.add(r); }); return out; }
        public List<TenantApiKeyRecord> findAllById(Iterable<String> ids) { return List.of(); }
        public void deleteAll() { store.clear(); }
        public void deleteAll(Iterable<? extends TenantApiKeyRecord> rs) {}
        public void deleteAllById(Iterable<? extends String> ids) {}
        public void flush() {}
        @SuppressWarnings("unchecked") public <S extends TenantApiKeyRecord> S saveAndFlush(S r) { return (S) save(r); }
        public <S extends TenantApiKeyRecord> List<S> saveAllAndFlush(Iterable<S> rs) { return saveAll(rs); }
        public void deleteAllInBatch() {}
        public void deleteAllInBatch(Iterable<TenantApiKeyRecord> rs) {}
        public void deleteAllByIdInBatch(Iterable<String> ids) {}
        public TenantApiKeyRecord getOne(String id) { return store.get(id); }
        public TenantApiKeyRecord getById(String id) { return store.get(id); }
        public TenantApiKeyRecord getReferenceById(String id) { return store.get(id); }
        public <S extends TenantApiKeyRecord> Optional<S> findOne(Example<S> ex) { return Optional.empty(); }
        public <S extends TenantApiKeyRecord> List<S> findAll(Example<S> ex) { return List.of(); }
        public <S extends TenantApiKeyRecord> List<S> findAll(Example<S> ex, Sort sort) { return List.of(); }
        public <S extends TenantApiKeyRecord> Page<S> findAll(Example<S> ex, Pageable p) { return Page.empty(); }
        public <S extends TenantApiKeyRecord> long count(Example<S> ex) { return 0; }
        public <S extends TenantApiKeyRecord> boolean exists(Example<S> ex) { return false; }
        public <S extends TenantApiKeyRecord, R> R findBy(Example<S> ex, Function<FluentQuery.FetchableFluentQuery<S>, R> fn) { return null; }
        public List<TenantApiKeyRecord> findAll(Sort sort) { return findAll(); }
        public Page<TenantApiKeyRecord> findAll(Pageable p) { return Page.empty(); }
    }
}
