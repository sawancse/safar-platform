package com.safar.payment.repository;

import com.safar.payment.entity.Payout;
import com.safar.payment.entity.enums.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface PayoutRepository extends JpaRepository<Payout, UUID> {
    List<Payout> findByHostId(UUID hostId);
    List<Payout> findByStatusAndScheduledAtBefore(PayoutStatus status, OffsetDateTime before);
    List<Payout> findByHostIdAndStatusIn(UUID hostId, List<PayoutStatus> statuses);
    List<Payout> findByStatus(PayoutStatus status);
}
