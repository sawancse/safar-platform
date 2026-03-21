package com.safar.user.service;

import com.safar.user.dto.MedicalProfileRequest;
import com.safar.user.dto.MedicalProfileResponse;
import com.safar.user.entity.MedicalProfile;
import com.safar.user.repository.MedicalProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicalProfileService {

    private final MedicalProfileRepository medicalProfileRepository;

    public Optional<MedicalProfileResponse> getProfile(UUID userId) {
        return medicalProfileRepository.findByUserId(userId)
                .map(this::toResponse);
    }

    @Transactional
    public MedicalProfileResponse createOrUpdate(UUID userId, MedicalProfileRequest request) {
        MedicalProfile profile = medicalProfileRepository.findByUserId(userId)
                .orElseGet(() -> MedicalProfile.builder().userId(userId).build());

        profile.setBloodGroup(request.bloodGroup());
        profile.setAllergies(request.allergies());
        profile.setCurrentMedications(request.currentMedications());
        profile.setPastSurgeries(request.pastSurgeries());
        profile.setChronicConditions(request.chronicConditions());
        profile.setEmergencyContactName(request.emergencyContactName());
        profile.setEmergencyContactPhone(request.emergencyContactPhone());
        profile.setEmergencyContactRelation(request.emergencyContactRelation());
        profile.setPreferredLanguage(request.preferredLanguage());
        profile.setDietaryRestrictions(request.dietaryRestrictions());
        profile.setMobilityNeeds(request.mobilityNeeds());
        profile.setInsuranceProvider(request.insuranceProvider());
        profile.setInsurancePolicyNumber(request.insurancePolicyNumber());

        return toResponse(medicalProfileRepository.save(profile));
    }

    @Transactional
    public void deleteProfile(UUID userId) {
        medicalProfileRepository.deleteByUserId(userId);
    }

    private MedicalProfileResponse toResponse(MedicalProfile entity) {
        return new MedicalProfileResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getBloodGroup(),
                entity.getAllergies(),
                entity.getCurrentMedications(),
                entity.getPastSurgeries(),
                entity.getChronicConditions(),
                entity.getEmergencyContactName(),
                entity.getEmergencyContactPhone(),
                entity.getEmergencyContactRelation(),
                entity.getPreferredLanguage(),
                entity.getDietaryRestrictions(),
                entity.getMobilityNeeds(),
                entity.getInsuranceProvider(),
                entity.getInsurancePolicyNumber(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
