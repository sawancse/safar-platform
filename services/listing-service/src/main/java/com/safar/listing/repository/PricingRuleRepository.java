package com.safar.listing.repository;

import com.safar.listing.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PricingRuleRepository extends JpaRepository<PricingRule, UUID> {
    List<PricingRule> findByListingIdAndIsActiveTrueOrderByPriorityDesc(UUID listingId);
    List<PricingRule> findByListingIdOrderByPriorityDesc(UUID listingId);
    List<PricingRule> findByListingIdAndRoomTypeIdAndIsActiveTrueOrderByPriorityDesc(UUID listingId, UUID roomTypeId);
}
