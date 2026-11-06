package com.safar.payment.repository;

import com.safar.payment.entity.SettlementPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controllers return {@link SettlementPlan} straight to JSON and open-in-view is off, so the
 * lazy {@code lines} collection must be fetched with the plan or Jackson fails with
 * "failed to lazily initialize a collection ... no Session".
 */
public interface SettlementPlanRepository extends JpaRepository<SettlementPlan, UUID> {

    @EntityGraph(attributePaths = "lines")
    Optional<SettlementPlan> findByBookingId(UUID bookingId);

    @Override
    @EntityGraph(attributePaths = "lines")
    Optional<SettlementPlan> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = "lines")
    List<SettlementPlan> findAll();
}
