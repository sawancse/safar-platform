package com.safar.user.service;

import com.safar.user.dto.BrokerProfileResponse;
import com.safar.user.dto.CreateBrokerProfileRequest;
import com.safar.user.entity.BrokerProfile;
import com.safar.user.entity.UserProfile;
import com.safar.user.entity.enums.BrokerSpecialization;
import com.safar.user.entity.enums.BrokerSubscriptionTier;
import com.safar.user.repository.BrokerProfileRepository;
import com.safar.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrokerProfileService {

    private final BrokerProfileRepository brokerRepository;
    private final ProfileRepository profileRepository;

    @Transactional
    public BrokerProfileResponse createProfile(CreateBrokerProfileRequest request, UUID userId) {
        if (brokerRepository.existsByUserId(userId)) {
            throw new IllegalStateException("Broker profile already exists for this user");
        }

        BrokerSpecialization spec = BrokerSpecialization.RESIDENTIAL;
        if (request.specialization() != null) {
            try {
                spec = BrokerSpecialization.valueOf(request.specialization().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // keep default
            }
        }

        BrokerProfile profile = BrokerProfile.builder()
                .userId(userId)
                .companyName(request.companyName())
                .reraAgentId(request.reraAgentId())
                .operatingCities(request.operatingCities())
                .specialization(spec)
                .experienceYears(request.experienceYears() != null ? request.experienceYears() : 0)
                .bio(request.bio())
                .website(request.website())
                .officeAddress(request.officeAddress())
                .officeCity(request.officeCity())
                .officeState(request.officeState())
                .officePincode(request.officePincode())
                .build();

        BrokerProfile saved = brokerRepository.save(profile);

        // Update user role to include BROKER capability
        profileRepository.findById(userId).ifPresent(userProfile -> {
            String currentRole = userProfile.getRole();
            if (currentRole == null || currentRole.isEmpty()) {
                userProfile.setRole("BROKER");
            } else if (!currentRole.contains("BROKER")) {
                userProfile.setRole(currentRole + ",BROKER");
            }
            profileRepository.save(userProfile);
        });

        log.info("Broker profile created for user {}", userId);
        return toResponse(saved);
    }

    public BrokerProfileResponse getProfile(UUID userId) {
        BrokerProfile profile = brokerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Broker profile not found"));
        return toResponse(profile);
    }

    public BrokerProfileResponse getProfileById(UUID id) {
        BrokerProfile profile = brokerRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Broker profile not found"));
        return toResponse(profile);
    }

    @Transactional
    public BrokerProfileResponse updateProfile(CreateBrokerProfileRequest request, UUID userId) {
        BrokerProfile profile = brokerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Broker profile not found"));

        if (request.companyName() != null) profile.setCompanyName(request.companyName());
        if (request.reraAgentId() != null) profile.setReraAgentId(request.reraAgentId());
        if (request.operatingCities() != null) profile.setOperatingCities(request.operatingCities());
        if (request.specialization() != null) {
            try {
                profile.setSpecialization(BrokerSpecialization.valueOf(request.specialization().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (request.experienceYears() != null) profile.setExperienceYears(request.experienceYears());
        if (request.bio() != null) profile.setBio(request.bio());
        if (request.website() != null) profile.setWebsite(request.website());
        if (request.officeAddress() != null) profile.setOfficeAddress(request.officeAddress());
        if (request.officeCity() != null) profile.setOfficeCity(request.officeCity());
        if (request.officeState() != null) profile.setOfficeState(request.officeState());
        if (request.officePincode() != null) profile.setOfficePincode(request.officePincode());

        BrokerProfile saved = brokerRepository.save(profile);
        log.info("Broker profile updated for user {}", userId);
        return toResponse(saved);
    }

    public Page<BrokerProfileResponse> searchBrokers(String city, String specialization, Pageable pageable) {
        Page<BrokerProfile> page;
        boolean hasCity = city != null && !city.isBlank();
        boolean hasSpec = specialization != null && !specialization.isBlank();

        if (hasCity && hasSpec) {
            page = brokerRepository.findByCityAndSpecialization(city, specialization.toUpperCase(), pageable);
        } else if (hasCity) {
            page = brokerRepository.findByCity(city, pageable);
        } else if (hasSpec) {
            page = brokerRepository.findBySpecialization(specialization.toUpperCase(), pageable);
        } else {
            page = brokerRepository.findByActiveTrue(pageable);
        }

        return page.map(this::toResponse);
    }

    public Page<BrokerProfileResponse> adminList(Pageable pageable) {
        return brokerRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public BrokerProfileResponse adminVerify(UUID id) {
        BrokerProfile profile = brokerRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Broker profile not found"));
        profile.setVerified(true);
        profile.setReraVerified(true);
        BrokerProfile saved = brokerRepository.save(profile);
        log.info("Broker {} verified by admin", id);
        return toResponse(saved);
    }

    private BrokerProfileResponse toResponse(BrokerProfile p) {
        // Enrich with user details
        String userName = null;
        String userPhone = null;
        String userEmail = null;
        String avatarUrl = null;

        UserProfile userProfile = profileRepository.findById(p.getUserId()).orElse(null);
        if (userProfile != null) {
            userName = userProfile.getName();
            userPhone = userProfile.getPhone();
            userEmail = userProfile.getEmail();
            avatarUrl = userProfile.getAvatarUrl();
        }

        return new BrokerProfileResponse(
                p.getId(),
                p.getUserId(),
                p.getCompanyName(),
                p.getReraAgentId(),
                p.getReraVerified(),
                p.getOperatingCities(),
                p.getSpecialization() != null ? p.getSpecialization().name() : "RESIDENTIAL",
                p.getExperienceYears(),
                p.getTotalDealsCount(),
                p.getBio(),
                p.getWebsite(),
                p.getOfficeAddress(),
                p.getOfficeCity(),
                p.getOfficeState(),
                p.getOfficePincode(),
                p.getSubscriptionTier() != null ? p.getSubscriptionTier().name() : "FREE",
                p.getVerified(),
                p.getActive(),
                userName,
                userPhone,
                userEmail,
                avatarUrl,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
