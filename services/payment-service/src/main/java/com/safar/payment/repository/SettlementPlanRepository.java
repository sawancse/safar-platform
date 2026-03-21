package com.safar.payment.repository;

import com.safar.payment.entity.SettlementPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SettlementPlanRepository extends JpaRepository<SettlementPlan, UUID> {
    Optional<SettlementPlan> findByBookingId(UUID bookingId);
}
