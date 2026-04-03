package com.safar.chef.service;

import com.safar.chef.entity.ChefProfile;
import com.safar.chef.entity.CuisinePriceTier;
import com.safar.chef.repository.ChefProfileRepository;
import com.safar.chef.repository.CuisinePriceTierRepository;
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
