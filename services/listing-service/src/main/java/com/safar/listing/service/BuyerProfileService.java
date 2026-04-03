package com.safar.listing.service;

import com.safar.listing.dto.BuyerProfileRequest;
import com.safar.listing.dto.BuyerProfileResponse;
import com.safar.listing.dto.SalePropertyResponse;
import com.safar.listing.entity.BuyerProfile;
import com.safar.listing.entity.SaleProperty;
import com.safar.listing.entity.enums.SalePropertyStatus;
import com.safar.listing.repository.BuyerProfileRepository;
import com.safar.listing.repository.SalePropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuyerProfileService {

    private final BuyerProfileRepository profileRepository;
    private final SalePropertyRepository salePropertyRepository;
    private final SalePropertyService salePropertyService;

    @Transactional
    public BuyerProfileResponse createOrUpdate(BuyerProfileRequest req, UUID userId) {
        BuyerProfile profile = profileRepository.findByUserId(userId)
                .orElse(BuyerProfile.builder().userId(userId).build());

        if (req.preferredCities() != null) profile.setPreferredCities(req.preferredCities());
        if (req.preferredLocalities() != null) profile.setPreferredLocalities(req.preferredLocalities());
        if (req.budgetMinPaise() != null) profile.setBudgetMinPaise(req.budgetMinPaise());
        if (req.budgetMaxPaise() != null) profile.setBudgetMaxPaise(req.budgetMaxPaise());
        if (req.preferredBhk() != null) profile.setPreferredBhk(req.preferredBhk());
        if (req.preferredTypes() != null) profile.setPreferredTypes(req.preferredTypes());
        if (req.financingType() != null) profile.setFinancingType(req.financingType());
        if (req.possessionTimeline() != null) profile.setPossessionTimeline(req.possessionTimeline());
        if (req.alertsEnabled() != null) profile.setAlertsEnabled(req.alertsEnabled());

        profile = profileRepository.save(profile);
        log.info("Buyer profile saved for user {}", userId);
        return toResponse(profile);
    }

    public BuyerProfileResponse getProfile(UUID userId) {
        BuyerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Buyer profile not found"));
        return toResponse(profile);
    }

    public List<SalePropertyResponse> getRecommendations(UUID userId) {
        BuyerProfile profile = profileRepository.findByUserId(userId).orElse(null);
        if (profile == null || profile.getPreferredCities() == null || profile.getPreferredCities().isEmpty()) {
            return List.of();
        }

        long minPrice = profile.getBudgetMinPaise() != null ? profile.getBudgetMinPaise() : 0L;
        long maxPrice = profile.getBudgetMaxPaise() != null ? profile.getBudgetMaxPaise() : Long.MAX_VALUE;

        List<SaleProperty> matches = salePropertyRepository.findMatchingProperties(
                profile.getPreferredCities(), minPrice, maxPrice, PageRequest.of(0, 20));

        return matches.stream().map(sp -> salePropertyService.getById(sp.getId())).toList();
    }

    private BuyerProfileResponse toResponse(BuyerProfile p) {
        return new BuyerProfileResponse(
                p.getId(), p.getUserId(),
                p.getPreferredCities(), p.getPreferredLocalities(),
                p.getBudgetMinPaise(), p.getBudgetMaxPaise(),
                p.getPreferredBhk(), p.getPreferredTypes(),
                p.getFinancingType(), p.getPossessionTimeline(),
                p.getAlertsEnabled(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
