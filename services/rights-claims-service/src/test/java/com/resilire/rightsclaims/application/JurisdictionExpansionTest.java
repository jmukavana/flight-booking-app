package com.resilire.rightsclaims.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.resilire.rightsclaims.domain.DisruptionCause;
import com.resilire.rightsclaims.domain.EntitlementType;
import com.resilire.rightsclaims.domain.Jurisdiction;
import com.resilire.rightsclaims.domain.RulesetStatus;
import com.resilire.rightsclaims.infrastructure.persistence.AssessmentJpaRepository;
import com.resilire.rightsclaims.infrastructure.persistence.AssessmentRecord;
import com.resilire.rightsclaims.infrastructure.persistence.EligibilityRuleJpaRepository;
import com.resilire.rightsclaims.infrastructure.persistence.EligibilityRuleRecord;
import com.resilire.rightsclaims.infrastructure.persistence.RulesetJpaRepository;
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

import java.util.function.Function;

class JurisdictionExpansionTest {

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
    }

    // ── UK261 ──────────────────────────────────────────────────────────────

    @Test
    void uk261_short_haul_delay_eligible_for_gbp220() {
        seedUk261();
        AssessmentRecord result = service.assess(new AssessmentRequest(
                "d-uk-1", "BA001", "LHR", "AMS", Jurisdiction.UK261, 200,
                DisruptionCause.CONTROLLABLE, 1200));

        assertThat(result.isEligible()).isTrue();
        assertThat(result.getCompensationAmount()).isEqualByComparingTo(new BigDecimal("220.00"));
        assertThat(result.getCurrency()).isEqualTo("GBP");
    }

    @Test
    void uk261_long_haul_over_4h_eligible_for_gbp520() {
        seedUk261();
        AssessmentRecord result = service.assess(new AssessmentRequest(
                "d-uk-2", "BA117", "LHR", "JFK", Jurisdiction.UK261, 270,
                DisruptionCause.CONTROLLABLE, 5500));

        assertThat(result.isEligible()).isTrue();
        assertThat(result.getCompensationAmount()).isEqualByComparingTo(new BigDecimal("520.00"));
    }

    // ── CANADA APPR ────────────────────────────────────────────────────────

    @Test
    void canada_appr_short_haul_3h_delay_eligible_for_cad400() {
        seedCanadaAppr();
        AssessmentRecord result = service.assess(new AssessmentRequest(
                "d-ca-1", "AC123", "YYZ", "YVR", Jurisdiction.CANADA_APPR, 210,
                DisruptionCause.CONTROLLABLE, 1100));

        assertThat(result.isEligible()).isTrue();
        assertThat(result.getCompensationAmount()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(result.getCurrency()).isEqualTo("CAD");
    }

    @Test
    void canada_appr_long_haul_eligible_for_cad1000() {
        seedCanadaAppr();
        AssessmentRecord result = service.assess(new AssessmentRequest(
                "d-ca-2", "AC856", "YYZ", "LHR", Jurisdiction.CANADA_APPR, 200,
                DisruptionCause.CONTROLLABLE, 5700));

        assertThat(result.isEligible()).isTrue();
        assertThat(result.getCompensationAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void canada_appr_uncontrollable_not_eligible() {
        seedCanadaAppr();
        AssessmentRecord result = service.assess(new AssessmentRequest(
                "d-ca-3", "AC999", "YYZ", "LHR", Jurisdiction.CANADA_APPR, 300,
                DisruptionCause.UNCONTROLLABLE, 5700));

        assertThat(result.isEligible()).isFalse();
    }

    // ── seed helpers ───────────────────────────────────────────────────────

    private void seedUk261() {
        RulesetRecord rs = new RulesetRecord();
        rs.setId("rs-uk261");
        rs.setJurisdiction(Jurisdiction.UK261);
        rs.setVersion("2024.1");
        rs.setStatus(RulesetStatus.ACTIVE);
        rs.setEffectiveFrom(LocalDate.of(2024, 1, 1));
        rs.setCreatedAt(Instant.now());
        rulesets.save(rs);

        rules.saveAll(List.of(
                rule("rs-uk261", DisruptionCause.CONTROLLABLE, 180, null, null, 1500, "220.00", "GBP"),
                rule("rs-uk261", DisruptionCause.CONTROLLABLE, 180, null, 1501, 3500, "350.00", "GBP"),
                rule("rs-uk261", DisruptionCause.CONTROLLABLE, 180, 239, 3501, null, "260.00", "GBP"),
                rule("rs-uk261", DisruptionCause.CONTROLLABLE, 240, null, 3501, null, "520.00", "GBP")));
    }

    private void seedCanadaAppr() {
        RulesetRecord rs = new RulesetRecord();
        rs.setId("rs-canada");
        rs.setJurisdiction(Jurisdiction.CANADA_APPR);
        rs.setVersion("2024.1");
        rs.setStatus(RulesetStatus.ACTIVE);
        rs.setEffectiveFrom(LocalDate.of(2024, 1, 1));
        rs.setCreatedAt(Instant.now());
        rulesets.save(rs);

        rules.saveAll(List.of(
                rule("rs-canada", DisruptionCause.CONTROLLABLE, 180, 359, null, 1500, "400.00", "CAD"),
                rule("rs-canada", DisruptionCause.CONTROLLABLE, 360, null, null, 1500, "700.00", "CAD"),
                rule("rs-canada", DisruptionCause.CONTROLLABLE, 180, null, 1501, 3500, "600.00", "CAD"),
                rule("rs-canada", DisruptionCause.CONTROLLABLE, 180, null, 3501, null, "1000.00", "CAD")));
    }

    private EligibilityRuleRecord rule(String rulesetId, DisruptionCause cause,
                                        int delayMin, Integer delayMax,
                                        Integer distMin, Integer distMax,
                                        String amount, String currency) {
        EligibilityRuleRecord r = new EligibilityRuleRecord();
        r.setId(UUID.randomUUID().toString());
        r.setRulesetId(rulesetId);
        r.setEntitlementType(EntitlementType.COMPENSATION);
        r.setCause(cause);
        r.setDelayMinMinutes(delayMin);
        r.setDelayMaxMinutes(delayMax);
        r.setFlightDistanceKmMin(distMin);
        r.setFlightDistanceKmMax(distMax);
        r.setCompensationAmount(new BigDecimal(amount));
        r.setCurrency(currency);
        r.setDescription("test rule");
        return r;
    }

    // --- minimal in-memory stubs (identical pattern to EligibilityServiceTest) ---

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
