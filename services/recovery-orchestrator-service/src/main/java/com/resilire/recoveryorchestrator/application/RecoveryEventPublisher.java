package com.resilire.recoveryorchestrator.application;

import com.resilire.recoveryorchestrator.domain.RecoveryOptionType;
import com.resilire.recoveryorchestrator.infrastructure.persistence.RecoveryCaseRecord;
import java.util.List;

public interface RecoveryEventPublisher {
    void publishCaseOpened(RecoveryCaseRecord c, List<RecoveryOptionType> options);
}
