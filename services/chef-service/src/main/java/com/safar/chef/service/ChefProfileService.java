package com.safar.chef.service;

import com.safar.chef.dto.CreateChefProfileRequest;
import com.safar.chef.dto.UpdateChefProfileRequest;
import com.safar.chef.entity.ChefProfile;
import com.safar.chef.entity.enums.ChefType;
import com.safar.chef.entity.enums.VerificationStatus;
import com.safar.chef.repository.ChefProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChefProfileService {

    private final ChefProfileRepository chefProfileRepo;

    @Transactional
    public ChefProfile registerChef(UUID userId, CreateChefProfileRequest req) {
        chefProfileRepo.findByUserId(userId).ifPresent(existing -> {
            throw new IllegalArgumentException("Chef profile already exists for this user");
        });

        ChefProfile profile = ChefProfile.builder()
                .userId(userId)
                .name(req.name())
                .phone(req.phone())
                .email(req.email())
                .bio(req.bio())
                .chefType(req.chefType() != null ? ChefType.valueOf(req.chefType()) : ChefType.DOMESTIC)
                .experienceYears(req.experienceYears() != null ? req.experienceYears() : 0)
                .city(req.city())
                .state(req.state())
                .pincode(req.pincode())
                .cuisines(req.cuisines())
                .specialties(req.specialties())
                .localities(req.localities())
                .dailyRatePaise(req.dailyRatePaise())
                .monthlyRatePaise(req.monthlyRatePaise())
                .eventMinPlatePaise(req.eventMinPlatePaise())
                .languages(req.languages())
                .eventMinPax(req.eventMinPax())
                .eventMaxPax(req.eventMaxPax())
                .verificationStatus(VerificationStatus.VERIFIED)
                .verified(true)
                .available(true)
                .rating(0.0)
                .reviewCount(0)
                .totalBookings(0)
                .completionRate(100.0)
                .build();

        ChefProfile saved = chefProfileRepo.save(profile);
        log.info("Chef profile registered: {} for userId={}", saved.getId(), userId);
        return saved;
    }

    @Transactional
    public ChefProfile updateProfile(UUID userId, UpdateChefProfileRequest req) {
        ChefProfile profile = chefProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));

        if (req.name() != null) profile.setName(req.name());
        if (req.phone() != null) profile.setPhone(req.phone());
        if (req.email() != null) profile.setEmail(req.email());
        if (req.bio() != null) profile.setBio(req.bio());
        if (req.chefType() != null) profile.setChefType(ChefType.valueOf(req.chefType()));
        if (req.experienceYears() != null) profile.setExperienceYears(req.experienceYears());
        if (req.city() != null) profile.setCity(req.city());
        if (req.state() != null) profile.setState(req.state());
        if (req.pincode() != null) profile.setPincode(req.pincode());
        if (req.cuisines() != null) profile.setCuisines(req.cuisines());
        if (req.specialties() != null) profile.setSpecialties(req.specialties());
        if (req.localities() != null) profile.setLocalities(req.localities());
        if (req.dailyRatePaise() != null) profile.setDailyRatePaise(req.dailyRatePaise());
        if (req.monthlyRatePaise() != null) profile.setMonthlyRatePaise(req.monthlyRatePaise());
        if (req.eventMinPlatePaise() != null) profile.setEventMinPlatePaise(req.eventMinPlatePaise());
        if (req.languages() != null) profile.setLanguages(req.languages());
        if (req.eventMinPax() != null) profile.setEventMinPax(req.eventMinPax());
        if (req.eventMaxPax() != null) profile.setEventMaxPax(req.eventMaxPax());

        log.info("Chef profile updated: {} for userId={}", profile.getId(), userId);
        return chefProfileRepo.save(profile);
    }

    @Transactional(readOnly = true)
    public ChefProfile getProfile(UUID chefId) {
        return chefProfileRepo.findById(chefId)
                .orElseThrow(() -> new IllegalArgumentException("Chef not found"));
    }

    @Transactional(readOnly = true)
    public ChefProfile getProfileByUserId(UUID userId) {
        return chefProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));
    }

    @Transactional(readOnly = true)
    public Page<ChefProfile> searchChefs(String city, String cuisine, String locality,
                                          String mealType, String chefType, Double minRating,
                                          Long maxPricePaise, int page, int size) {
        Specification<ChefProfile> spec = Specification.where(null);

        // Only show available and verified chefs in public search
        spec = spec.and((root, query, cb) -> cb.equal(root.get("available"), true));
        spec = spec.and((root, query, cb) -> cb.equal(root.get("verificationStatus"), VerificationStatus.VERIFIED));

        if (city != null && !city.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("city")), city.toLowerCase()));
        }
        if (cuisine != null && !cuisine.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("cuisines")), "%" + cuisine.toLowerCase() + "%"));
        }
        if (locality != null && !locality.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("localities")), "%" + locality.toLowerCase() + "%"));
        }
        if (chefType != null && !chefType.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("chefType"), ChefType.valueOf(chefType)));
        }
        if (minRating != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("rating"), minRating));
        }
        if (maxPricePaise != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("dailyRatePaise"), maxPricePaise));
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "rating"));
        return chefProfileRepo.findAll(spec, pageable);
    }

    @Transactional
    public ChefProfile verifyChef(UUID chefId) {
        ChefProfile profile = chefProfileRepo.findById(chefId)
                .orElseThrow(() -> new IllegalArgumentException("Chef not found"));
        profile.setVerificationStatus(VerificationStatus.VERIFIED);
        profile.setVerified(true);
        log.info("Chef {} verified by admin", chefId);
        return chefProfileRepo.save(profile);
    }

    @Transactional
    public ChefProfile rejectChef(UUID chefId, String reason) {
        ChefProfile profile = chefProfileRepo.findById(chefId)
                .orElseThrow(() -> new IllegalArgumentException("Chef not found"));
        profile.setVerificationStatus(VerificationStatus.REJECTED);
        log.info("Chef {} rejected by admin. Reason: {}", chefId, reason);
        return chefProfileRepo.save(profile);
    }

    @Transactional
    public ChefProfile suspendChef(UUID chefId) {
        ChefProfile profile = chefProfileRepo.findById(chefId)
                .orElseThrow(() -> new IllegalArgumentException("Chef not found"));
        profile.setVerificationStatus(VerificationStatus.SUSPENDED);
        profile.setAvailable(false);
        log.info("Chef {} suspended by admin", chefId);
        return chefProfileRepo.save(profile);
    }

    @Transactional(readOnly = true)
    public Page<ChefProfile> getPendingChefs(int page, int size) {
        return chefProfileRepo.findAll(
                (root, query, cb) -> cb.equal(root.get("verificationStatus"), VerificationStatus.PENDING),
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public Page<ChefProfile> getAllChefs(int page, int size) {
        return chefProfileRepo.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional
    public ChefProfile toggleAvailability(UUID userId) {
        ChefProfile profile = chefProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));
        profile.setAvailable(!profile.getAvailable());
        log.info("Chef {} availability toggled to {}", profile.getId(), profile.getAvailable());
        return chefProfileRepo.save(profile);
    }
}
