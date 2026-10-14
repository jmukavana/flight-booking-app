package com.resilire.partner.application;

import com.resilire.partner.domain.ReferralStatus;
import com.resilire.partner.infrastructure.persistence.ReferralJpaRepository;
import com.resilire.partner.infrastructure.persistence.ReferralRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReferralServiceTest {

    private ReferralService service;

    @BeforeEach
    void setUp() {
        service = new ReferralService(new StubReferralRepo());
    }

    @Test
    void create_referral_starts_as_pending() {
        ReferralRecord r = service.createReferral("referrer-1", "referred-1");
        assertThat(r.getStatus()).isEqualTo(ReferralStatus.PENDING);
        assertThat(r.getReferrerId()).isEqualTo("referrer-1");
        assertThat(r.getReferredId()).isEqualTo("referred-1");
        assertThat(r.getCreatedAt()).isNotNull();
    }

    @Test
    void create_referral_is_idempotent() {
        ReferralRecord first = service.createReferral("referrer-2", "referred-2");
        ReferralRecord second = service.createReferral("referrer-2", "referred-2");
        assertThat(first.getId()).isEqualTo(second.getId());
    }

    @Test
    void credit_referral_transitions_to_credited() {
        ReferralRecord r = service.createReferral("referrer-3", "referred-3");
        ReferralRecord credited = service.creditReferral(r.getId());
        assertThat(credited.getStatus()).isEqualTo(ReferralStatus.CREDITED);
        assertThat(credited.getCreditedAt()).isNotNull();
    }

    @Test
    void credit_already_credited_referral_throws() {
        ReferralRecord r = service.createReferral("referrer-4", "referred-4");
        service.creditReferral(r.getId());
        assertThatThrownBy(() -> service.creditReferral(r.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    static class StubReferralRepo implements ReferralJpaRepository {
        private final Map<String, ReferralRecord> store = new HashMap<>();

        @Override
        public List<ReferralRecord> findByReferrerId(String referrerId) {
            return store.values().stream().filter(r -> r.getReferrerId().equals(referrerId)).toList();
        }

        @Override
        public boolean existsByReferrerIdAndReferredId(String referrerId, String referredId) {
            return store.values().stream()
                    .anyMatch(r -> r.getReferrerId().equals(referrerId) && r.getReferredId().equals(referredId));
        }

        @Override
        public <S extends ReferralRecord> S save(S r) {
            store.put(r.getId(), r);
            return r;
        }

        @Override public <S extends ReferralRecord> List<S> saveAll(Iterable<S> rs) {
            List<S> out = new ArrayList<>();
            rs.forEach(r -> { store.put(r.getId(), r); out.add(r); });
            return out;
        }
        @Override public Optional<ReferralRecord> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public boolean existsById(String id) { return store.containsKey(id); }
        @Override public List<ReferralRecord> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<ReferralRecord> findAllById(Iterable<String> ids) { return List.of(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(String id) { store.remove(id); }
        @Override public void delete(ReferralRecord r) { store.remove(r.getId()); }
        @Override public void deleteAllById(Iterable<? extends String> ids) { ids.forEach(store::remove); }
        @Override public void deleteAll(Iterable<? extends ReferralRecord> rs) { rs.forEach(r -> store.remove(r.getId())); }
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @SuppressWarnings("unchecked")
        @Override public <S extends ReferralRecord> S saveAndFlush(S r) { return (S) save(r); }
        @Override public <S extends ReferralRecord> List<S> saveAllAndFlush(Iterable<S> rs) { return saveAll(rs); }
        @Override public void deleteAllInBatch(Iterable<ReferralRecord> rs) {}
        @Override public void deleteAllByIdInBatch(Iterable<String> ids) {}
        @Override public void deleteAllInBatch() { store.clear(); }
        @Override public ReferralRecord getOne(String id) { return store.get(id); }
        @Override public ReferralRecord getById(String id) { return store.get(id); }
        @Override public ReferralRecord getReferenceById(String id) { return store.get(id); }
        @Override public <S extends ReferralRecord> Optional<S> findOne(org.springframework.data.domain.Example<S> ex) { return Optional.empty(); }
        @Override public <S extends ReferralRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex) { return List.of(); }
        @Override public <S extends ReferralRecord> List<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public <S extends ReferralRecord> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends ReferralRecord> long count(org.springframework.data.domain.Example<S> ex) { return 0; }
        @Override public <S extends ReferralRecord> boolean exists(org.springframework.data.domain.Example<S> ex) { return false; }
        @Override public <S extends ReferralRecord, R> R findBy(org.springframework.data.domain.Example<S> ex, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> q) { return null; }
        @Override public org.springframework.data.domain.Page<ReferralRecord> findAll(org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public List<ReferralRecord> findAll(org.springframework.data.domain.Sort sort) { return new ArrayList<>(store.values()); }
    }
}
