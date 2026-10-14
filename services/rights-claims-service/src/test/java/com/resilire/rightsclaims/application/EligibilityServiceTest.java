package com.resilire.rightsclaims.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.resilire.rightsclaims.domain.DisruptionCause;
import com.resilire.rightsclaims.domain.EntitlementType;
import com.resilire.rightsclaims.domain.Jurisdiction;
import com.resilire.rightsclaims.domain.RulesetStatus;
import com.resilire.rightsclaims.infrastructure.persistence.AssessmentRecord;
import com.resilire.rightsclaims.infrastructure.persistence.EligibilityRuleRecord;
import com.resilire.rightsclaims.infrastructure.persistence.RulesetRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import com.resilire.rightsclaims.infrastructure.persistence.AssessmentJpaRepository;
import com.resilire.rightsclaims.infrastructure.persistence.EligibilityRuleJpaRepository;
import com.resilire.rightsclaims.infrastructure.persistence.RulesetJpaRepository;

import java.util.function.Function;

class EligibilityServiceTest {

    private InMemoryRulesetRepo rulesets;
    private InMemoryRuleRepo rules;
    private InMemoryAssessmentRepo assessments;
    private EligibilityService service;

    @BeforeEach
    void setUp() {
        rulesets = new InMemoryRulesetRepo();
        rules = new InMemoryRuleRepo();
        assessments = new InMemoryAssessmentRepo();
        service = new EligibilityService(rulesets, rules, assessments);
        seedEu261();
    }

    @Test
    void eu261_short_haul_controllable_delay_eligible_for_250eur() {
        AssessmentRecord result = service.assess(new AssessmentRequest(
                "d-1", "KQ101", "NBO", "LHR", Jurisdiction.EU261, 200,
                DisruptionCause.CONTROLLABLE, 1200));

        assertThat(result.isEligible()).isTrue();
        assertThat(result.getEntitlementType()).isEqualTo(EntitlementType.COMPENSATION);
        assertThat(result.getCompensationAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(result.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void eu261_long_haul_over_4h_delay_eligible_for_600eur() {
        AssessmentRecord result = service.assess(new AssessmentRequest(
                "d-2", "EK001", "DXB", "LHR", Jurisdiction.EU261, 270,
                DisruptionCause.CONTROLLABLE, 5500));

        assertThat(result.isEligible()).isTrue();
        assertThat(result.getCompensationAmount()).isEqualByComparingTo(new BigDecimal("600.00"));
    }

    @Test
    void uncontrollable_disruption_not_eligible() {
        AssessmentRecord result = service.assess(new AssessmentRequest(
                "d-3", "KQ202", "NBO", "LHR", Jurisdiction.EU261, 300,
                DisruptionCause.UNCONTROLLABLE, 1200));

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getReasoning()).contains("No rule matches");
    }

    @Test
    void delay_below_threshold_not_eligible() {
        AssessmentRecord result = service.assess(new AssessmentRequest(
                "d-4", "KQ303", "NBO", "MBA", Jurisdiction.EU261, 120,
                DisruptionCause.CONTROLLABLE, 400));

        assertThat(result.isEligible()).isFalse();
    }

    @Test
    void duplicate_disruption_id_returns_existing_assessment() {
        service.assess(new AssessmentRequest("d-5", "KQ404", "NBO", "LHR",
                Jurisdiction.EU261, 200, DisruptionCause.CONTROLLABLE, 1200));
        AssessmentRecord second = service.assess(new AssessmentRequest("d-5", "KQ404", "NBO", "LHR",
                Jurisdiction.EU261, 200, DisruptionCause.CONTROLLABLE, 1200));

        assertThat(assessments.store).hasSize(1);
        assertThat(second.getDisruptionId()).isEqualTo("d-5");
    }

    @Test
    void no_active_ruleset_yields_ineligible_with_reason() {
        AssessmentRecord result = service.assess(new AssessmentRequest(
                "d-6", "AC001", "YYZ", "LHR", Jurisdiction.CANADA_APPR, 300,
                DisruptionCause.CONTROLLABLE, 5000));

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getReasoning()).contains("No active ruleset");
    }

    private void seedEu261() {
        RulesetRecord rs = new RulesetRecord();
        rs.setId("rs-eu261");
        rs.setJurisdiction(Jurisdiction.EU261);
        rs.setVersion("2024.1");
        rs.setStatus(RulesetStatus.ACTIVE);
        rs.setEffectiveFrom(LocalDate.of(2024, 1, 1));
        rs.setCreatedAt(Instant.now());
        rulesets.save(rs);

        rules.saveAll(List.of(
                rule("rs-eu261", EntitlementType.COMPENSATION, DisruptionCause.CONTROLLABLE,
                        180, null, null, 1500, "250.00"),
                rule("rs-eu261", EntitlementType.COMPENSATION, DisruptionCause.CONTROLLABLE,
                        180, null, 1501, 3500, "400.00"),
                rule("rs-eu261", EntitlementType.COMPENSATION, DisruptionCause.CONTROLLABLE,
                        180, 239, 3501, null, "300.00"),
                rule("rs-eu261", EntitlementType.COMPENSATION, DisruptionCause.CONTROLLABLE,
                        240, null, 3501, null, "600.00")));
    }

    private EligibilityRuleRecord rule(String rulesetId, EntitlementType type,
                                       DisruptionCause cause, int delayMin, Integer delayMax,
                                       Integer distMin, Integer distMax, String amount) {
        EligibilityRuleRecord r = new EligibilityRuleRecord();
        r.setId(UUID.randomUUID().toString());
        r.setRulesetId(rulesetId);
        r.setEntitlementType(type);
        r.setCause(cause);
        r.setDelayMinMinutes(delayMin);
        r.setDelayMaxMinutes(delayMax);
        r.setFlightDistanceKmMin(distMin);
        r.setFlightDistanceKmMax(distMax);
        r.setCompensationAmount(new BigDecimal(amount));
        r.setCurrency("EUR");
        r.setDescription("test rule");
        return r;
    }

    // --- minimal in-memory stubs ---

    private static class InMemoryRulesetRepo implements RulesetJpaRepository {
        final Map<String, RulesetRecord> store = new HashMap<>();
        public RulesetRecord save(RulesetRecord r) { store.put(r.getId(), r); return r; }
        public Optional<RulesetRecord> findByJurisdictionAndStatus(Jurisdiction j, RulesetStatus s) {
            return store.values().stream().filter(r -> r.getJurisdiction() == j && r.getStatus() == s).findFirst();
        }
        public boolean existsByJurisdictionAndStatus(Jurisdiction j, RulesetStatus s) {
            return store.values().stream().anyMatch(r -> r.getJurisdiction() == j && r.getStatus() == s);
        }
        public Optional<RulesetRecord> findById(String id) { return Optional.ofNullable(store.get(id)); }
        public List<RulesetRecord> findAll() { return new ArrayList<>(store.values()); }
        public boolean existsById(String id) { return store.containsKey(id); }
        public void deleteById(String id) { store.remove(id); }
        public void delete(RulesetRecord r) { store.remove(r.getId()); }
        public long count() { return store.size(); }
        public <S extends RulesetRecord> List<S> saveAll(Iterable<S> rs) { List<S> out = new ArrayList<>(); rs.forEach(r -> { store.put(r.getId(), r); out.add(r); }); return out; }
        public List<RulesetRecord> findAllById(Iterable<String> ids) { return List.of(); }
        public void deleteAll() { store.clear(); }
        public void deleteAll(Iterable<? extends RulesetRecord> rs) {}
        public void deleteAllById(Iterable<? extends String> ids) {}
        public void flush() {}
        @SuppressWarnings("unchecked") public <S extends RulesetRecord> S saveAndFlush(S r) { return (S) save(r); }
        public <S extends RulesetRecord> List<S> saveAllAndFlush(Iterable<S> rs) { return saveAll(rs); }
        public void deleteAllInBatch() {}
        public void deleteAllInBatch(Iterable<RulesetRecord> rs) {}
        public void deleteAllByIdInBatch(Iterable<String> ids) {}
        public RulesetRecord getOne(String id) { return store.get(id); }
        public RulesetRecord getById(String id) { return store.get(id); }
        public RulesetRecord getReferenceById(String id) { return store.get(id); }
        public <S extends RulesetRecord> Optional<S> findOne(Example<S> ex) { return Optional.empty(); }
        public <S extends RulesetRecord> List<S> findAll(Example<S> ex) { return List.of(); }
        public <S extends RulesetRecord> List<S> findAll(Example<S> ex, Sort sort) { return List.of(); }
        public <S extends RulesetRecord> Page<S> findAll(Example<S> ex, Pageable p) { return Page.empty(); }
        public <S extends RulesetRecord> long count(Example<S> ex) { return 0; }
        public <S extends RulesetRecord> boolean exists(Example<S> ex) { return false; }
        public <S extends RulesetRecord, R> R findBy(Example<S> ex, Function<FluentQuery.FetchableFluentQuery<S>, R> fn) { return null; }
        public List<RulesetRecord> findByJurisdiction(Jurisdiction j) { return store.values().stream().filter(r -> r.getJurisdiction() == j).toList(); }
        public List<RulesetRecord> findByStatusAndEffectiveFromLessThanEqual(RulesetStatus s, java.time.LocalDate date) { return store.values().stream().filter(r -> r.getStatus() == s && !r.getEffectiveFrom().isAfter(date)).toList(); }
        public List<RulesetRecord> findAll(Sort sort) { return findAll(); }
        public Page<RulesetRecord> findAll(Pageable p) { return Page.empty(); }
    }

    private static class InMemoryRuleRepo implements EligibilityRuleJpaRepository {
        final Map<String, EligibilityRuleRecord> store = new HashMap<>();
        public EligibilityRuleRecord save(EligibilityRuleRecord r) { store.put(r.getId(), r); return r; }
        public List<EligibilityRuleRecord> findByRulesetId(String id) {
            return store.values().stream().filter(r -> r.getRulesetId().equals(id)).toList();
        }
        public <S extends EligibilityRuleRecord> List<S> saveAll(Iterable<S> rs) { List<S> out = new ArrayList<>(); rs.forEach(r -> { store.put(r.getId(), r); out.add(r); }); return out; }
        public Optional<EligibilityRuleRecord> findById(String id) { return Optional.ofNullable(store.get(id)); }
        public List<EligibilityRuleRecord> findAll() { return new ArrayList<>(store.values()); }
        public boolean existsById(String id) { return store.containsKey(id); }
        public void deleteById(String id) { store.remove(id); }
        public void delete(EligibilityRuleRecord r) { store.remove(r.getId()); }
        public long count() { return store.size(); }
        public List<EligibilityRuleRecord> findAllById(Iterable<String> ids) { return List.of(); }
        public void deleteAll() { store.clear(); }
        public void deleteAll(Iterable<? extends EligibilityRuleRecord> rs) {}
        public void deleteAllById(Iterable<? extends String> ids) {}
        public void flush() {}
        @SuppressWarnings("unchecked") public <S extends EligibilityRuleRecord> S saveAndFlush(S r) { return (S) save(r); }
        public <S extends EligibilityRuleRecord> List<S> saveAllAndFlush(Iterable<S> rs) { return saveAll(rs); }
        public void deleteAllInBatch() {}
        public void deleteAllInBatch(Iterable<EligibilityRuleRecord> rs) {}
        public void deleteAllByIdInBatch(Iterable<String> ids) {}
        public EligibilityRuleRecord getOne(String id) { return store.get(id); }
        public EligibilityRuleRecord getById(String id) { return store.get(id); }
        public EligibilityRuleRecord getReferenceById(String id) { return store.get(id); }
        public <S extends EligibilityRuleRecord> Optional<S> findOne(Example<S> ex) { return Optional.empty(); }
        public <S extends EligibilityRuleRecord> List<S> findAll(Example<S> ex) { return List.of(); }
        public <S extends EligibilityRuleRecord> List<S> findAll(Example<S> ex, Sort sort) { return List.of(); }
        public <S extends EligibilityRuleRecord> Page<S> findAll(Example<S> ex, Pageable p) { return Page.empty(); }
        public <S extends EligibilityRuleRecord> long count(Example<S> ex) { return 0; }
        public <S extends EligibilityRuleRecord> boolean exists(Example<S> ex) { return false; }
        public <S extends EligibilityRuleRecord, R> R findBy(Example<S> ex, Function<FluentQuery.FetchableFluentQuery<S>, R> fn) { return null; }
        public List<EligibilityRuleRecord> findAll(Sort sort) { return findAll(); }
        public Page<EligibilityRuleRecord> findAll(Pageable p) { return Page.empty(); }
    }

    private static class InMemoryAssessmentRepo implements AssessmentJpaRepository {
        final Map<String, AssessmentRecord> store = new HashMap<>();
        public AssessmentRecord save(AssessmentRecord r) { store.put(r.getId(), r); return r; }
        public Optional<AssessmentRecord> findByDisruptionId(String id) {
            return store.values().stream().filter(r -> id.equals(r.getDisruptionId())).findFirst();
        }
        public Optional<AssessmentRecord> findById(String id) { return Optional.ofNullable(store.get(id)); }
        public List<AssessmentRecord> findAll() { return new ArrayList<>(store.values()); }
        public boolean existsById(String id) { return store.containsKey(id); }
        public void deleteById(String id) { store.remove(id); }
        public void delete(AssessmentRecord r) { store.remove(r.getId()); }
        public long count() { return store.size(); }
        public <S extends AssessmentRecord> List<S> saveAll(Iterable<S> rs) { List<S> out = new ArrayList<>(); rs.forEach(r -> { store.put(r.getId(), r); out.add(r); }); return out; }
        public List<AssessmentRecord> findAllById(Iterable<String> ids) { return List.of(); }
        public void deleteAll() { store.clear(); }
        public void deleteAll(Iterable<? extends AssessmentRecord> rs) {}
        public void deleteAllById(Iterable<? extends String> ids) {}
        public void flush() {}
        @SuppressWarnings("unchecked") public <S extends AssessmentRecord> S saveAndFlush(S r) { return (S) save(r); }
        public <S extends AssessmentRecord> List<S> saveAllAndFlush(Iterable<S> rs) { return saveAll(rs); }
        public void deleteAllInBatch() {}
        public void deleteAllInBatch(Iterable<AssessmentRecord> rs) {}
        public void deleteAllByIdInBatch(Iterable<String> ids) {}
        public AssessmentRecord getOne(String id) { return store.get(id); }
        public AssessmentRecord getById(String id) { return store.get(id); }
        public AssessmentRecord getReferenceById(String id) { return store.get(id); }
        public <S extends AssessmentRecord> Optional<S> findOne(Example<S> ex) { return Optional.empty(); }
        public <S extends AssessmentRecord> List<S> findAll(Example<S> ex) { return List.of(); }
        public <S extends AssessmentRecord> List<S> findAll(Example<S> ex, Sort sort) { return List.of(); }
        public <S extends AssessmentRecord> Page<S> findAll(Example<S> ex, Pageable p) { return Page.empty(); }
        public <S extends AssessmentRecord> long count(Example<S> ex) { return 0; }
        public <S extends AssessmentRecord> boolean exists(Example<S> ex) { return false; }
        public <S extends AssessmentRecord, R> R findBy(Example<S> ex, Function<FluentQuery.FetchableFluentQuery<S>, R> fn) { return null; }
        public List<AssessmentRecord> findAll(Sort sort) { return findAll(); }
        public Page<AssessmentRecord> findAll(Pageable p) { return Page.empty(); }
    }
}
