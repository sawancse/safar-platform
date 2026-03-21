package com.safar.listing.service;

import com.safar.listing.dto.PricingRuleRequest;
import com.safar.listing.dto.PricingRuleResponse;
import com.safar.listing.entity.PricingRule;
import com.safar.listing.repository.ListingRepository;
import com.safar.listing.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingRuleService {

    private final PricingRuleRepository pricingRuleRepository;
    private final ListingRepository listingRepository;

    @Transactional
    public PricingRuleResponse createRule(UUID listingId, PricingRuleRequest req) {
        if (!listingRepository.existsById(listingId)) {
            throw new NoSuchElementException("Listing not found: " + listingId);
        }

        PricingRule rule = PricingRule.builder()
                .listingId(listingId)
                .roomTypeId(req.roomTypeId())
                .name(req.name())
                .ruleType(req.ruleType())
                .fromDate(req.fromDate())
                .toDate(req.toDate())
                .daysOfWeek(req.daysOfWeek())
                .priceAdjustmentType(req.priceAdjustmentType())
                .adjustmentValue(req.adjustmentValue())
                .priority(req.priority() != null ? req.priority() : 0)
                .isActive(req.isActive() != null ? req.isActive() : true)
                .build();

        return toResponse(pricingRuleRepository.save(rule));
    }

    public List<PricingRuleResponse> getRules(UUID listingId) {
        return pricingRuleRepository.findByListingIdOrderByPriorityDesc(listingId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public PricingRuleResponse updateRule(UUID listingId, UUID ruleId, PricingRuleRequest req) {
        PricingRule rule = pricingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new NoSuchElementException("Pricing rule not found: " + ruleId));

        if (!rule.getListingId().equals(listingId)) {
            throw new IllegalArgumentException("Rule does not belong to this listing");
        }

        rule.setName(req.name());
        rule.setRuleType(req.ruleType());
        rule.setRoomTypeId(req.roomTypeId());
        rule.setFromDate(req.fromDate());
        rule.setToDate(req.toDate());
        rule.setDaysOfWeek(req.daysOfWeek());
        rule.setPriceAdjustmentType(req.priceAdjustmentType());
        rule.setAdjustmentValue(req.adjustmentValue());
        if (req.priority() != null) rule.setPriority(req.priority());
        if (req.isActive() != null) rule.setIsActive(req.isActive());

        return toResponse(pricingRuleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(UUID listingId, UUID ruleId) {
        PricingRule rule = pricingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new NoSuchElementException("Pricing rule not found: " + ruleId));

        if (!rule.getListingId().equals(listingId)) {
            throw new IllegalArgumentException("Rule does not belong to this listing");
        }

        pricingRuleRepository.delete(rule);
    }

    /**
     * Calculate effective price for a given listing, room type, date, and base price.
     * Applies matching active rules in priority order (highest priority first).
     * Only the first matching rule is applied.
     */
    public Long calculateEffectivePrice(UUID listingId, UUID roomTypeId, LocalDate date, Long basePricePaise) {
        List<PricingRule> rules;
        if (roomTypeId != null) {
            rules = pricingRuleRepository.findByListingIdAndRoomTypeIdAndIsActiveTrueOrderByPriorityDesc(listingId, roomTypeId);
            // Also include rules with null roomTypeId (listing-level rules)
            List<PricingRule> listingLevelRules = pricingRuleRepository.findByListingIdAndIsActiveTrueOrderByPriorityDesc(listingId)
                    .stream().filter(r -> r.getRoomTypeId() == null).toList();
            // Merge: room-specific rules first, then listing-level
            rules = new java.util.ArrayList<>(rules);
            rules.addAll(listingLevelRules);
        } else {
            rules = pricingRuleRepository.findByListingIdAndIsActiveTrueOrderByPriorityDesc(listingId);
        }

        long effectivePrice = basePricePaise;

        for (PricingRule rule : rules) {
            if (!matchesRule(rule, date)) continue;

            effectivePrice = applyAdjustment(effectivePrice, rule.getPriceAdjustmentType(), rule.getAdjustmentValue());
            // Apply only the first matching rule (highest priority)
            break;
        }

        return Math.max(effectivePrice, 0);
    }

    private boolean matchesRule(PricingRule rule, LocalDate date) {
        String type = rule.getRuleType();

        switch (type) {
            case "SEASONAL":
                return rule.getFromDate() != null && rule.getToDate() != null
                        && !date.isBefore(rule.getFromDate()) && !date.isAfter(rule.getToDate());

            case "WEEKEND":
                if (rule.getDaysOfWeek() != null && !rule.getDaysOfWeek().isBlank()) {
                    Set<DayOfWeek> dows = Arrays.stream(rule.getDaysOfWeek().split(","))
                            .map(String::trim)
                            .map(DayOfWeek::valueOf)
                            .collect(Collectors.toSet());
                    return dows.contains(date.getDayOfWeek());
                }
                // Default weekend: Saturday and Sunday
                return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;

            case "LAST_MINUTE":
                long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), date);
                return daysUntil >= 0 && daysUntil <= 3;

            case "EARLY_BIRD":
                long daysAhead = ChronoUnit.DAYS.between(LocalDate.now(), date);
                return daysAhead >= 30;

            default:
                log.warn("Unknown pricing rule type: {}", type);
                return false;
        }
    }

    private long applyAdjustment(long basePaise, String adjustmentType, long adjustmentValue) {
        switch (adjustmentType) {
            case "FIXED_PRICE":
                return adjustmentValue;
            case "PERCENT_INCREASE":
                return basePaise + (basePaise * adjustmentValue / 100);
            case "PERCENT_DECREASE":
                return basePaise - (basePaise * adjustmentValue / 100);
            default:
                log.warn("Unknown adjustment type: {}", adjustmentType);
                return basePaise;
        }
    }

    private PricingRuleResponse toResponse(PricingRule r) {
        return new PricingRuleResponse(
                r.getId(), r.getListingId(), r.getRoomTypeId(),
                r.getName(), r.getRuleType(),
                r.getFromDate(), r.getToDate(), r.getDaysOfWeek(),
                r.getPriceAdjustmentType(), r.getAdjustmentValue(),
                r.getPriority(), r.getIsActive(), r.getCreatedAt()
        );
    }
}
