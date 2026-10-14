package com.resilire.rightsclaims.application;

import com.resilire.rightsclaims.domain.ClaimStatus;
import com.resilire.rightsclaims.infrastructure.persistence.AssessmentJpaRepository;
import com.resilire.rightsclaims.infrastructure.persistence.AssessmentRecord;
import com.resilire.rightsclaims.infrastructure.persistence.ClaimJpaRepository;
import com.resilire.rightsclaims.infrastructure.persistence.ClaimRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaimApplicationServiceTest {

    private StubClaimRepo claimRepo;
    private StubAssessmentRepo assessmentRepo;
    private StubEventPublisher publisher;
    private ClaimApplicationService service;

    @BeforeEach
    void setUp() {
        claimRepo = new StubClaimRepo();
        assessmentRepo = new StubAssessmentRepo();
        publisher = new StubEventPublisher();
        service = new ClaimApplicationService(claimRepo, assessmentRepo, publisher);
    }

    @Test
    void submit_eligible_assessment_creates_pending_claim() {
        AssessmentRecord assessment = eligibleAssessment("assess-1");
        assessmentRepo.save(assessment);

        ClaimRecord claim = service.submit("assess-1", "traveller-1");

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(claim.getTravellerId()).isEqualTo("traveller-1");
        assertThat(claim.getAssessmentId()).isEqualTo("assess-1");
        assertThat(publisher.submitted).hasSize(1);
    }

    @Test
    void submit_ineligible_assessment_throws() {
        AssessmentRecord assessment = ineligibleAssessment("assess-2");
        assessmentRepo.save(assessment);

        assertThatThrownBy(() -> service.submit("assess-2", "traveller-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ineligible");
    }

    @Test
    void submit_is_idempotent_on_assessmentId() {
        assessmentRepo.save(eligibleAssessment("assess-3"));
        ClaimRecord first = service.submit("assess-3", "traveller-1");
        ClaimRecord second = service.submit("assess-3", "traveller-1");

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(claimRepo.store).hasSize(1);
    }

    @Test
    void approve_transitions_to_approved_and_publishes_event() {
        assessmentRepo.save(eligibleAssessment("assess-4"));
        ClaimRecord claim = service.submit("assess-4", "traveller-1");
        publisher.submitted.clear();

        ClaimRecord approved = service.approve(claim.getId(), "Verified via EU261");

        assertThat(approved.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(approved.getNotes()).isEqualTo("Verified via EU261");
        assertThat(publisher.approved).hasSize(1);
    }

    @Test
    void reject_transitions_to_rejected() {
        assessmentRepo.save(eligibleAssessment("assess-5"));
        ClaimRecord claim = service.submit("assess-5", "traveller-1");

        ClaimRecord rejected = service.reject(claim.getId(), "Extraordinary circumstances");

        assertThat(rejected.getStatus()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(rejected.getNotes()).isEqualTo("Extraordinary circumstances");
    }

    @Test
    void mark_paid_transitions_approved_claim_to_paid() {
        assessmentRepo.save(eligibleAssessment("assess-6"));
        ClaimRecord claim = service.submit("assess-6", "traveller-1");
        service.approve(claim.getId(), "Approved");
        publisher.paid.clear();

        ClaimRecord paid = service.markPaid(claim.getId(), "payout_ch_001");

        assertThat(paid.getStatus()).isEqualTo(ClaimStatus.PAID);
        assertThat(paid.getPaymentRef()).isEqualTo("payout_ch_001");
        assertThat(publisher.paid).hasSize(1);
    }

    @Test
    void mark_paid_on_non_approved_claim_throws() {
        assessmentRepo.save(eligibleAssessment("assess-7"));
        ClaimRecord claim = service.submit("assess-7", "traveller-1");

        assertThatThrownBy(() -> service.markPaid(claim.getId(), "payout_ref"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPROVED");
    }

    private AssessmentRecord eligibleAssessment(String id) {
        AssessmentRecord r = new AssessmentRecord();
        r.setId(id);
        r.setEligible(true);
        r.setAssessedAt(Instant.now());
        return r;
    }

    private AssessmentRecord ineligibleAssessment(String id) {
        AssessmentRecord r = new AssessmentRecord();
        r.setId(id);
        r.setEligible(false);
        r.setAssessedAt(Instant.now());
        return r;
    }

    // --- stubs ---

    static class StubClaimRepo implements ClaimJpaRepository {
        final Map<String, ClaimRecord> store = new HashMap<>();

        @Override public <S extends ClaimRecord> S save(S r) { store.put(r.getId(), r); return r; }
        @Override public Optional<ClaimRecord> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<ClaimRecord> findByTravellerId(String t) {
            return store.values().stream().filter(r -> r.getTravellerId().equals(t)).toList();
        }
        @Override public List<ClaimRecord> findByAssessmentId(String a) {
            return store.values().stream().filter(r -> r.getAssessmentId().equals(a)).toList();
        }
        @Override public boolean existsByAssessmentIdAndStatus(String a, ClaimStatus s) {
            return store.values().stream().anyMatch(r -> r.getAssessmentId().equals(a) && r.getStatus() == s);
        }
        @Override public <S extends ClaimRecord> List<S> saveAll(Iterable<S> rs) {
            List<S> out = new ArrayList<>();
            rs.forEach(r -> { store.put(r.getId(), r); out.add(r); });
            return out;
        }
        @Override public boolean existsById(String id) { return store.containsKey(id); }
        @Override public List<ClaimRecord> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<ClaimRecord> findAllById(Iterable<String> ids) { return List.of(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(String id) { store.remove(id); }
        @Override public void delete(ClaimRecord r) { store.remove(r.getId()); }
        @Override public void deleteAllById(Iterable<? extends String> ids) { ids.forEach(store::remove); }
        @Override public void deleteAll(Iterable<? extends ClaimRecord> rs) { rs.forEach(r -> store.remove(r.getId())); }
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @SuppressWarnings("unchecked") @Override public <S extends ClaimRecord> S saveAndFlush(S r) { return (S) save(r); }
        @Override public <S extends ClaimRecord> List<S> saveAllAndFlush(Iterable<S> rs) { return saveAll(rs); }
        @Override public void deleteAllInBatch(Iterable<ClaimRecord> rs) {}
        @Override public void deleteAllByIdInBatch(Iterable<String> ids) {}
        @Override public void deleteAllInBatch() { store.clear(); }
        @Override public ClaimRecord getOne(String id) { return store.get(id); }
        @Override public ClaimRecord getById(String id) { return store.get(id); }
        @Override public ClaimRecord getReferenceById(String id) { return store.get(id); }
        @Override public <S extends ClaimRecord> Optional<S> findOne(org.springframework.data.domain.Example<S> ex) { return Optional.empty(); }
        @Override public <S extends ClaimRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex) { return List.of(); }
        @Override public <S extends ClaimRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Sort s) { return List.of(); }
        @Override public <S extends ClaimRecord> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends ClaimRecord> long count(org.springframework.data.domain.Example<S> ex) { return 0; }
        @Override public <S extends ClaimRecord> boolean exists(org.springframework.data.domain.Example<S> ex) { return false; }
        @Override public <S extends ClaimRecord, R> R findBy(org.springframework.data.domain.Example<S> ex, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> q) { return null; }
        @Override public org.springframework.data.domain.Page<ClaimRecord> findAll(org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public List<ClaimRecord> findAll(org.springframework.data.domain.Sort s) { return new ArrayList<>(store.values()); }
    }

    static class StubAssessmentRepo implements AssessmentJpaRepository {
        private final Map<String, AssessmentRecord> store = new HashMap<>();

        @Override public <S extends AssessmentRecord> S save(S r) { store.put(r.getId(), r); return r; }
        @Override public Optional<AssessmentRecord> findByDisruptionId(String d) {
            return store.values().stream().filter(r -> d.equals(r.getDisruptionId())).findFirst();
        }
        @Override public Optional<AssessmentRecord> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public <S extends AssessmentRecord> List<S> saveAll(Iterable<S> rs) {
            List<S> out = new ArrayList<>();
            rs.forEach(r -> { store.put(r.getId(), r); out.add(r); });
            return out;
        }
        @Override public boolean existsById(String id) { return store.containsKey(id); }
        @Override public List<AssessmentRecord> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<AssessmentRecord> findAllById(Iterable<String> ids) { return List.of(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(String id) { store.remove(id); }
        @Override public void delete(AssessmentRecord r) { store.remove(r.getId()); }
        @Override public void deleteAllById(Iterable<? extends String> ids) { ids.forEach(store::remove); }
        @Override public void deleteAll(Iterable<? extends AssessmentRecord> rs) { rs.forEach(r -> store.remove(r.getId())); }
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @SuppressWarnings("unchecked") @Override public <S extends AssessmentRecord> S saveAndFlush(S r) { return (S) save(r); }
        @Override public <S extends AssessmentRecord> List<S> saveAllAndFlush(Iterable<S> rs) { return saveAll(rs); }
        @Override public void deleteAllInBatch(Iterable<AssessmentRecord> rs) {}
        @Override public void deleteAllByIdInBatch(Iterable<String> ids) {}
        @Override public void deleteAllInBatch() { store.clear(); }
        @Override public AssessmentRecord getOne(String id) { return store.get(id); }
        @Override public AssessmentRecord getById(String id) { return store.get(id); }
        @Override public AssessmentRecord getReferenceById(String id) { return store.get(id); }
        @Override public <S extends AssessmentRecord> Optional<S> findOne(org.springframework.data.domain.Example<S> ex) { return Optional.empty(); }
        @Override public <S extends AssessmentRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex) { return List.of(); }
        @Override public <S extends AssessmentRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Sort s) { return List.of(); }
        @Override public <S extends AssessmentRecord> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends AssessmentRecord> long count(org.springframework.data.domain.Example<S> ex) { return 0; }
        @Override public <S extends AssessmentRecord> boolean exists(org.springframework.data.domain.Example<S> ex) { return false; }
        @Override public <S extends AssessmentRecord, R> R findBy(org.springframework.data.domain.Example<S> ex, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> q) { return null; }
        @Override public org.springframework.data.domain.Page<AssessmentRecord> findAll(org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public List<AssessmentRecord> findAll(org.springframework.data.domain.Sort s) { return new ArrayList<>(store.values()); }
    }

    static class StubEventPublisher implements ClaimEventPublisher {
        final List<ClaimRecord> submitted = new ArrayList<>();
        final List<ClaimRecord> approved = new ArrayList<>();
        final List<ClaimRecord> paid = new ArrayList<>();

        @Override public void publishClaimSubmitted(ClaimRecord c) { submitted.add(c); }
        @Override public void publishClaimApproved(ClaimRecord c) { approved.add(c); }
        @Override public void publishClaimPaid(ClaimRecord c) { paid.add(c); }
    }
}
