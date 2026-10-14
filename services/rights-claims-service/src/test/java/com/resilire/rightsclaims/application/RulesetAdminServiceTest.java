package com.resilire.rightsclaims.application;

import com.resilire.rightsclaims.domain.DisruptionCause;
import com.resilire.rightsclaims.domain.EntitlementType;
import com.resilire.rightsclaims.domain.Jurisdiction;
import com.resilire.rightsclaims.domain.RulesetStatus;
import com.resilire.rightsclaims.infrastructure.persistence.EligibilityRuleJpaRepository;
import com.resilire.rightsclaims.infrastructure.persistence.EligibilityRuleRecord;
import com.resilire.rightsclaims.infrastructure.persistence.RulesetJpaRepository;
import com.resilire.rightsclaims.infrastructure.persistence.RulesetRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RulesetAdminServiceTest {

    private RulesetAdminService service;
    private StubRulesetRepo rulesetRepo;
    private StubEligibilityRuleRepo ruleRepo;

    @BeforeEach
    void setUp() {
        rulesetRepo = new StubRulesetRepo();
        ruleRepo = new StubEligibilityRuleRepo();
        service = new RulesetAdminService(rulesetRepo, ruleRepo);
    }

    @Test
    void create_draft_persists_with_draft_status() {
        RulesetRecord r = service.createDraft(Jurisdiction.EU261, "2024.1", LocalDate.of(2024, 6, 1));
        assertThat(r.getStatus()).isEqualTo(RulesetStatus.DRAFT);
        assertThat(r.getJurisdiction()).isEqualTo(Jurisdiction.EU261);
        assertThat(r.getVersion()).isEqualTo("2024.1");
        assertThat(r.getId()).isNotNull();
    }

    @Test
    void activate_transitions_draft_to_active_and_supersedes_existing() {
        RulesetRecord old = service.createDraft(Jurisdiction.EU261, "v1", LocalDate.now());
        service.activate(old.getId());

        RulesetRecord newer = service.createDraft(Jurisdiction.EU261, "v2", LocalDate.now());
        service.activate(newer.getId());

        assertThat(rulesetRepo.findById(old.getId()).get().getStatus()).isEqualTo(RulesetStatus.SUPERSEDED);
        assertThat(rulesetRepo.findById(newer.getId()).get().getStatus()).isEqualTo(RulesetStatus.ACTIVE);
    }

    @Test
    void add_rule_only_allowed_on_draft() {
        RulesetRecord r = service.createDraft(Jurisdiction.EU261, "v1", LocalDate.now());
        service.activate(r.getId());

        assertThatThrownBy(() -> service.addRule(r.getId(), EntitlementType.COMPENSATION,
                DisruptionCause.CONTROLLABLE, 180, null, null, null,
                BigDecimal.valueOf(250), "EUR", "test rule"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void supersede_requires_active_status() {
        RulesetRecord r = service.createDraft(Jurisdiction.UK261, "v1", LocalDate.now());

        assertThatThrownBy(() -> service.supersede(r.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void find_all_returns_rulesets_for_jurisdiction() {
        service.createDraft(Jurisdiction.EU261, "v1", LocalDate.now());
        service.createDraft(Jurisdiction.EU261, "v2", LocalDate.now());
        service.createDraft(Jurisdiction.UK261, "v1", LocalDate.now());

        List<RulesetRecord> eu261 = service.findAll(Jurisdiction.EU261);
        assertThat(eu261).hasSize(2);
        assertThat(eu261).allMatch(r -> r.getJurisdiction() == Jurisdiction.EU261);
    }

    static class StubRulesetRepo implements RulesetJpaRepository {
        final Map<String, RulesetRecord> store = new HashMap<>();

        @Override public Optional<RulesetRecord> findByJurisdictionAndStatus(Jurisdiction j, RulesetStatus s) {
            return store.values().stream()
                    .filter(r -> r.getJurisdiction() == j && r.getStatus() == s)
                    .findFirst();
        }
        @Override public boolean existsByJurisdictionAndStatus(Jurisdiction j, RulesetStatus s) {
            return store.values().stream().anyMatch(r -> r.getJurisdiction() == j && r.getStatus() == s);
        }
        @Override public List<RulesetRecord> findByJurisdiction(Jurisdiction j) {
            return store.values().stream().filter(r -> r.getJurisdiction() == j).toList();
        }
        @Override public List<RulesetRecord> findByStatusAndEffectiveFromLessThanEqual(RulesetStatus s, LocalDate date) {
            return store.values().stream()
                    .filter(r -> r.getStatus() == s && !r.getEffectiveFrom().isAfter(date))
                    .toList();
        }
        @Override public <S extends RulesetRecord> S save(S r) { store.put(r.getId(), r); return r; }
        @Override public <S extends RulesetRecord> List<S> saveAll(Iterable<S> rs) {
            List<S> out = new ArrayList<>();
            rs.forEach(r -> { store.put(r.getId(), r); out.add(r); });
            return out;
        }
        @Override public Optional<RulesetRecord> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public boolean existsById(String id) { return store.containsKey(id); }
        @Override public List<RulesetRecord> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<RulesetRecord> findAllById(Iterable<String> ids) { return List.of(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(String id) { store.remove(id); }
        @Override public void delete(RulesetRecord r) { store.remove(r.getId()); }
        @Override public void deleteAllById(Iterable<? extends String> ids) { ids.forEach(store::remove); }
        @Override public void deleteAll(Iterable<? extends RulesetRecord> rs) { rs.forEach(r -> store.remove(r.getId())); }
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @SuppressWarnings("unchecked")
        @Override public <S extends RulesetRecord> S saveAndFlush(S r) { return (S) save(r); }
        @Override public <S extends RulesetRecord> List<S> saveAllAndFlush(Iterable<S> rs) { return saveAll(rs); }
        @Override public void deleteAllInBatch(Iterable<RulesetRecord> rs) {}
        @Override public void deleteAllByIdInBatch(Iterable<String> ids) {}
        @Override public void deleteAllInBatch() { store.clear(); }
        @Override public RulesetRecord getOne(String id) { return store.get(id); }
        @Override public RulesetRecord getById(String id) { return store.get(id); }
        @Override public RulesetRecord getReferenceById(String id) { return store.get(id); }
        @Override public <S extends RulesetRecord> Optional<S> findOne(org.springframework.data.domain.Example<S> ex) { return Optional.empty(); }
        @Override public <S extends RulesetRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex) { return List.of(); }
        @Override public <S extends RulesetRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public <S extends RulesetRecord> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends RulesetRecord> long count(org.springframework.data.domain.Example<S> ex) { return 0; }
        @Override public <S extends RulesetRecord> boolean exists(org.springframework.data.domain.Example<S> ex) { return false; }
        @Override public <S extends RulesetRecord, R> R findBy(org.springframework.data.domain.Example<S> ex, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> q) { return null; }
        @Override public org.springframework.data.domain.Page<RulesetRecord> findAll(org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public List<RulesetRecord> findAll(org.springframework.data.domain.Sort sort) { return new ArrayList<>(store.values()); }
    }

    static class StubEligibilityRuleRepo implements EligibilityRuleJpaRepository {
        private final Map<String, EligibilityRuleRecord> store = new HashMap<>();

        @Override public List<EligibilityRuleRecord> findByRulesetId(String rulesetId) {
            return store.values().stream().filter(r -> r.getRulesetId().equals(rulesetId)).toList();
        }
        @Override public <S extends EligibilityRuleRecord> S save(S r) { store.put(r.getId(), r); return r; }
        @Override public <S extends EligibilityRuleRecord> List<S> saveAll(Iterable<S> rs) {
            List<S> out = new ArrayList<>();
            rs.forEach(r -> { store.put(r.getId(), r); out.add(r); });
            return out;
        }
        @Override public Optional<EligibilityRuleRecord> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public boolean existsById(String id) { return store.containsKey(id); }
        @Override public List<EligibilityRuleRecord> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<EligibilityRuleRecord> findAllById(Iterable<String> ids) { return List.of(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(String id) { store.remove(id); }
        @Override public void delete(EligibilityRuleRecord r) { store.remove(r.getId()); }
        @Override public void deleteAllById(Iterable<? extends String> ids) { ids.forEach(store::remove); }
        @Override public void deleteAll(Iterable<? extends EligibilityRuleRecord> rs) { rs.forEach(r -> store.remove(r.getId())); }
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @SuppressWarnings("unchecked")
        @Override public <S extends EligibilityRuleRecord> S saveAndFlush(S r) { return (S) save(r); }
        @Override public <S extends EligibilityRuleRecord> List<S> saveAllAndFlush(Iterable<S> rs) { return saveAll(rs); }
        @Override public void deleteAllInBatch(Iterable<EligibilityRuleRecord> rs) {}
        @Override public void deleteAllByIdInBatch(Iterable<String> ids) {}
        @Override public void deleteAllInBatch() { store.clear(); }
        @Override public EligibilityRuleRecord getOne(String id) { return store.get(id); }
        @Override public EligibilityRuleRecord getById(String id) { return store.get(id); }
        @Override public EligibilityRuleRecord getReferenceById(String id) { return store.get(id); }
        @Override public <S extends EligibilityRuleRecord> Optional<S> findOne(org.springframework.data.domain.Example<S> ex) { return Optional.empty(); }
        @Override public <S extends EligibilityRuleRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex) { return List.of(); }
        @Override public <S extends EligibilityRuleRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public <S extends EligibilityRuleRecord> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends EligibilityRuleRecord> long count(org.springframework.data.domain.Example<S> ex) { return 0; }
        @Override public <S extends EligibilityRuleRecord> boolean exists(org.springframework.data.domain.Example<S> ex) { return false; }
        @Override public <S extends EligibilityRuleRecord, R> R findBy(org.springframework.data.domain.Example<S> ex, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> q) { return null; }
        @Override public org.springframework.data.domain.Page<EligibilityRuleRecord> findAll(org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public List<EligibilityRuleRecord> findAll(org.springframework.data.domain.Sort sort) { return new ArrayList<>(store.values()); }
    }
}
