package com.safar.services.service;

import com.safar.services.entity.ChefProfile;
import com.safar.services.entity.CuisinePriceTier;
import com.safar.services.repository.ChefProfileRepository;
import com.safar.services.repository.CuisinePriceTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CuisinePricingService {

    private final CuisinePriceTierRepository tierRepo;
    private final ChefProfileRepository chefProfileRepo;

    @Transactional
    public CuisinePriceTier setPricing(UUID userId, String cuisineType, Long pricePerPlatePaise) {
        ChefProfile chef = chefProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));

        CuisinePriceTier tier = tierRepo.findByChefIdAndCuisineType(chef.getId(), cuisineType)
                .orElse(CuisinePriceTier.builder().chefId(chef.getId()).cuisineType(cuisineType).build());
        tier.setPricePerPlatePaise(pricePerPlatePaise);
        return tierRepo.save(tier);
    }

    @Transactional(readOnly = true)
    public List<CuisinePriceTier> getChefPricing(UUID chefId) {
        return tierRepo.findByChefId(chefId);
    }

    @Transactional
    public void deletePricing(UUID userId, String cuisineType) {
        ChefProfile chef = chefProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));
        tierRepo.findByChefIdAndCuisineType(chef.getId(), cuisineType).ifPresent(tierRepo::delete);
    }
}
