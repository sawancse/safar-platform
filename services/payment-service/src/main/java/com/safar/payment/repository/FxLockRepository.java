package com.safar.payment.repository;

import com.safar.payment.entity.FxLock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FxLockRepository extends JpaRepository<FxLock, UUID> {

    Optional<FxLock> findByBookingId(UUID bookingId);

    Optional<FxLock> findByIdAndUsedFalse(UUID id);
}
