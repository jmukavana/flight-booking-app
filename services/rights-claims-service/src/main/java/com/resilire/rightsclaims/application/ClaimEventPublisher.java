package com.resilire.rightsclaims.application;

import com.resilire.rightsclaims.infrastructure.persistence.ClaimRecord;

public interface ClaimEventPublisher {
    void publishClaimSubmitted(ClaimRecord claim);
    void publishClaimApproved(ClaimRecord claim);
    void publishClaimPaid(ClaimRecord claim);
}
