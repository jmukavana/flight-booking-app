package com.resilire.recoveryorchestrator.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.resilire.recoveryorchestrator.domain.RecoveryCaseStatus;
import com.resilire.recoveryorchestrator.domain.RecoveryOptionType;
import com.resilire.recoveryorchestrator.infrastructure.persistence.RecoveryCaseJpaRepository;
import com.resilire.recoveryorchestrator.infrastructure.persistence.RecoveryCaseRecord;
import com.resilire.recoveryorchestrator.infrastructure.persistence.RecoveryOptionJpaRepository;
import com.resilire.recoveryorchestrator.infrastructure.persistence.RecoveryOptionRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.function.Function;

class RecoveryOrchestratorServiceTest {

    private InMemoryCaseRepo caseRepo;
    private InMemoryOptionRepo optionRepo;
    private NoOpPublisher publisher;
    private RecoveryOrchestratorService service;

    @BeforeEach
    void setUp() {
        caseRepo = new InMemoryCaseRepo();
        optionRepo = new InMemoryOptionRepo();
        publisher = new NoOpPublisher();
        service = new RecoveryOrchestratorService(caseRepo, optionRepo, publisher, false);
    }

    @Test
    void disruption_detected_opens_recovery_case() {
        RecoveryCaseRecord result = service.openCase("d-1", "KQ101", "NBO", "LHR",
                "SIGNIFICANT_DELAY", "MODERATE", 90);

        assertThat(result.getDisruptionId()).isEqualTo("d-1");
        assertThat(result.getStatus()).isEqualTo(RecoveryCaseStatus.OPEN);
        assertThat(result.getOpenedAt()).isNotNull();
        assertThat(publisher.publishedCount).isEqualTo(1);
    }

    @Test
    void duplicate_disruption_id_returns_existing_case() {
        service.openCase("d-2", "KQ102", "NBO", "LHR", "SIGNIFICANT_DELAY", "MODERATE", 90);
        service.openCase("d-2", "KQ102", "NBO", "LHR", "SIGNIFICANT_DELAY", "MODERATE", 90);

        assertThat(caseRepo.store).hasSize(1);
        assertThat(publisher.publishedCount).isEqualTo(1);
    }

    @Test
    void severe_delay_proposes_rebooking_and_meal_voucher() {
        RecoveryCaseRecord c = service.openCase("d-3", "EK001", "DXB", "LHR",
                "SIGNIFICANT_DELAY", "SEVERE", 150);

        List<RecoveryOptionRecord> opts = optionRepo.findByCaseId(c.getId());
        List<RecoveryOptionType> types = opts.stream().map(RecoveryOptionRecord::getOptionType).toList();
        assertThat(types).containsExactlyInAnyOrder(
                RecoveryOptionType.REBOOKING, RecoveryOptionType.MEAL_VOUCHER);
    }

    @Test
    void cancelled_disruption_proposes_refund_and_rebooking() {
        RecoveryCaseRecord c = service.openCase("d-4", "BA001", "LHR", "JFK",
                "FLIGHT_CANCELLED", "CANCELLED", 0);

        List<RecoveryOptionType> types = optionRepo.findByCaseId(c.getId()).stream()
                .map(RecoveryOptionRecord::getOptionType).toList();
        assertThat(types).containsExactlyInAnyOrder(
                RecoveryOptionType.FULL_REFUND, RecoveryOptionType.REBOOKING);
    }

    @Test
    void moderate_delay_proposes_meal_voucher_only() {
        RecoveryCaseRecord c = service.openCase("d-5", "KQ303", "NBO", "MBA",
                "SIGNIFICANT_DELAY", "MODERATE", 60);

        List<RecoveryOptionType> types = optionRepo.findByCaseId(c.getId()).stream()
                .map(RecoveryOptionRecord::getOptionType).toList();
        assertThat(types).containsExactly(RecoveryOptionType.MEAL_VOUCHER);
    }

    @Test
    void resolve_case_transitions_to_resolved() {
        RecoveryCaseRecord c = service.openCase("d-6", "KQ404", "NBO", "LHR",
                "SIGNIFICANT_DELAY", "SEVERE", 130);

        RecoveryCaseRecord resolved = service.resolveCase(c.getId());

        assertThat(resolved.getStatus()).isEqualTo(RecoveryCaseStatus.RESOLVED);
        assertThat(resolved.getResolvedAt()).isNotNull();
    }

    @Test
    void cancelled_short_haul_adds_ground_transport_option() {
        RecoveryCaseRecord c = service.openCase("d-7", "KQ105", "NBO", "MBA",
                "FLIGHT_CANCELLED", "CANCELLED", 0, 480);

        List<RecoveryOptionType> types = optionRepo.findByCaseId(c.getId()).stream()
                .map(RecoveryOptionRecord::getOptionType).toList();
        assertThat(types).contains(RecoveryOptionType.GROUND_TRANSPORT);
    }

    @Test
    void severe_4h_plus_delay_adds_hotel_accommodation() {
        RecoveryCaseRecord c = service.openCase("d-8", "EK002", "DXB", "LHR",
                "SIGNIFICANT_DELAY", "SEVERE", 270, 5500);

        List<RecoveryOptionType> types = optionRepo.findByCaseId(c.getId()).stream()
                .map(RecoveryOptionRecord::getOptionType).toList();
        assertThat(types).contains(RecoveryOptionType.HOTEL_ACCOMMODATION);
    }

    // --- in-memory stubs ---

    private static class NoOpPublisher implements RecoveryEventPublisher {
        int publishedCount = 0;
        @Override
        public void publishCaseOpened(RecoveryCaseRecord c, List<RecoveryOptionType> options) {
            publishedCount++;
        }
    }

    private static class InMemoryCaseRepo implements RecoveryCaseJpaRepository {
        final Map<String, RecoveryCaseRecord> store = new HashMap<>();
        public RecoveryCaseRecord save(RecoveryCaseRecord r) { store.put(r.getId(), r); return r; }
        public Optional<RecoveryCaseRecord> findByDisruptionId(String id) {
            return store.values().stream().filter(r -> id.equals(r.getDisruptionId())).findFirst();
        }
        public List<RecoveryCaseRecord> findByStatus(RecoveryCaseStatus s) {
            return store.values().stream().filter(r -> r.getStatus() == s).toList();
        }
        public Optional<RecoveryCaseRecord> findById(String id) { return Optional.ofNullable(store.get(id)); }
        public List<RecoveryCaseRecord> findAll() { return new ArrayList<>(store.values()); }
        public boolean existsById(String id) { return store.containsKey(id); }
        public void deleteById(String id) { store.remove(id); }
        public void delete(RecoveryCaseRecord r) { store.remove(r.getId()); }
        public long count() { return store.size(); }
        public <S extends RecoveryCaseRecord> List<S> saveAll(Iterable<S> rs) { List<S> out = new ArrayList<>(); rs.forEach(r -> { store.put(r.getId(), r); out.add(r); }); return out; }
        public List<RecoveryCaseRecord> findAllById(Iterable<String> ids) { return List.of(); }
        public void deleteAll() { store.clear(); }
        public void deleteAll(Iterable<? extends RecoveryCaseRecord> rs) {}
        public void deleteAllById(Iterable<? extends String> ids) {}
        public void flush() {}
        @SuppressWarnings("unchecked") public <S extends RecoveryCaseRecord> S saveAndFlush(S r) { return (S) save(r); }
        public <S extends RecoveryCaseRecord> List<S> saveAllAndFlush(Iterable<S> rs) { return saveAll(rs); }
        public void deleteAllInBatch() {}
        public void deleteAllInBatch(Iterable<RecoveryCaseRecord> rs) {}
        public void deleteAllByIdInBatch(Iterable<String> ids) {}
        public RecoveryCaseRecord getOne(String id) { return store.get(id); }
        public RecoveryCaseRecord getById(String id) { return store.get(id); }
        public RecoveryCaseRecord getReferenceById(String id) { return store.get(id); }
        public <S extends RecoveryCaseRecord> Optional<S> findOne(Example<S> ex) { return Optional.empty(); }
        public <S extends RecoveryCaseRecord> List<S> findAll(Example<S> ex) { return List.of(); }
        public <S extends RecoveryCaseRecord> List<S> findAll(Example<S> ex, Sort sort) { return List.of(); }
        public <S extends RecoveryCaseRecord> Page<S> findAll(Example<S> ex, Pageable p) { return Page.empty(); }
        public <S extends RecoveryCaseRecord> long count(Example<S> ex) { return 0; }
        public <S extends RecoveryCaseRecord> boolean exists(Example<S> ex) { return false; }
        public <S extends RecoveryCaseRecord, R> R findBy(Example<S> ex, Function<FluentQuery.FetchableFluentQuery<S>, R> fn) { return null; }
        public List<RecoveryCaseRecord> findAll(Sort sort) { return findAll(); }
        public Page<RecoveryCaseRecord> findAll(Pageable p) { return Page.empty(); }
    }

    private static class InMemoryOptionRepo implements RecoveryOptionJpaRepository {
        final Map<String, RecoveryOptionRecord> store = new HashMap<>();
        public RecoveryOptionRecord save(RecoveryOptionRecord r) { store.put(r.getId(), r); return r; }
        public List<RecoveryOptionRecord> findByCaseId(String caseId) {
            return store.values().stream().filter(r -> caseId.equals(r.getCaseId())).toList();
        }
        public Optional<RecoveryOptionRecord> findById(String id) { return Optional.ofNullable(store.get(id)); }
        public List<RecoveryOptionRecord> findAll() { return new ArrayList<>(store.values()); }
        public boolean existsById(String id) { return store.containsKey(id); }
        public void deleteById(String id) { store.remove(id); }
        public void delete(RecoveryOptionRecord r) { store.remove(r.getId()); }
        public long count() { return store.size(); }
        public <S extends RecoveryOptionRecord> List<S> saveAll(Iterable<S> rs) { List<S> out = new ArrayList<>(); rs.forEach(r -> { store.put(r.getId(), r); out.add(r); }); return out; }
        public List<RecoveryOptionRecord> findAllById(Iterable<String> ids) { return List.of(); }
        public void deleteAll() { store.clear(); }
        public void deleteAll(Iterable<? extends RecoveryOptionRecord> rs) {}
        public void deleteAllById(Iterable<? extends String> ids) {}
        public void flush() {}
        @SuppressWarnings("unchecked") public <S extends RecoveryOptionRecord> S saveAndFlush(S r) { return (S) save(r); }
        public <S extends RecoveryOptionRecord> List<S> saveAllAndFlush(Iterable<S> rs) { return saveAll(rs); }
        public void deleteAllInBatch() {}
        public void deleteAllInBatch(Iterable<RecoveryOptionRecord> rs) {}
        public void deleteAllByIdInBatch(Iterable<String> ids) {}
        public RecoveryOptionRecord getOne(String id) { return store.get(id); }
        public RecoveryOptionRecord getById(String id) { return store.get(id); }
        public RecoveryOptionRecord getReferenceById(String id) { return store.get(id); }
        public <S extends RecoveryOptionRecord> Optional<S> findOne(Example<S> ex) { return Optional.empty(); }
        public <S extends RecoveryOptionRecord> List<S> findAll(Example<S> ex) { return List.of(); }
        public <S extends RecoveryOptionRecord> List<S> findAll(Example<S> ex, Sort sort) { return List.of(); }
        public <S extends RecoveryOptionRecord> Page<S> findAll(Example<S> ex, Pageable p) { return Page.empty(); }
        public <S extends RecoveryOptionRecord> long count(Example<S> ex) { return 0; }
        public <S extends RecoveryOptionRecord> boolean exists(Example<S> ex) { return false; }
        public <S extends RecoveryOptionRecord, R> R findBy(Example<S> ex, Function<FluentQuery.FetchableFluentQuery<S>, R> fn) { return null; }
        public List<RecoveryOptionRecord> findAll(Sort sort) { return findAll(); }
        public Page<RecoveryOptionRecord> findAll(Pageable p) { return Page.empty(); }
    }
}
