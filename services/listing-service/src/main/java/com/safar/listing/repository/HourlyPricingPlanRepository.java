package com.safar.listing.repository;

import com.safar.listing.entity.HourlyPricingPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HourlyPricingPlanRepository extends JpaRepository<HourlyPricingPlan, UUID> {
    List<HourlyPricingPlan> findByRoomTypeIdOrderBySlotHours(UUID roomTypeId);
}
