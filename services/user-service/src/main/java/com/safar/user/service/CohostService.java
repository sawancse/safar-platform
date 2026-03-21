package com.safar.user.service;

import com.safar.user.dto.CohostAgreementRequest;
import com.safar.user.dto.CohostProfileRequest;
import com.safar.user.entity.CohostAgreement;
import com.safar.user.entity.CohostEarnings;
import com.safar.user.entity.CohostProfile;
import com.safar.user.entity.enums.AgreementStatus;
import com.safar.user.repository.CohostAgreementRepository;
import com.safar.user.repository.CohostEarningsRepository;
import com.safar.user.repository.CohostProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CohostService {

    private final CohostProfileRepository profileRepo;
    private final CohostAgreementRepository agreementRepo;
    private final CohostEarningsRepository earningsRepo;

    @Transactional
    public CohostProfile createProfile(UUID hostId, CohostProfileRequest req) {
        if (profileRepo.existsByHostId(hostId)) {
            throw new IllegalStateException("Cohost profile already exists for host: " + hostId);
        }

        int maxFeePct = req.maxFeePct() != null ? req.maxFeePct() : 15;
        if (maxFeePct > 20) {
            maxFeePct = 20;
        }

        CohostProfile profile = CohostProfile.builder()
                .hostId(hostId)
                .bio(req.bio())
                .servicesOffered(req.servicesOffered())
                .cities(req.cities())
                .minFeePct(req.minFeePct() != null ? req.minFeePct() : 5)
                .maxFeePct(maxFeePct)
                .maxListings(req.maxListings() != null ? req.maxListings() : 5)
                .build();

        return profileRepo.save(profile);
    }

    @Transactional
    public CohostProfile updateProfile(UUID hostId, CohostProfileRequest req) {
        CohostProfile profile = profileRepo.findByHostId(hostId)
                .orElseThrow(() -> new NoSuchElementException("Cohost profile not found for host: " + hostId));

        if (req.bio() != null) profile.setBio(req.bio());
        if (req.servicesOffered() != null) profile.setServicesOffered(req.servicesOffered());
        if (req.cities() != null) profile.setCities(req.cities());
        if (req.minFeePct() != null) profile.setMinFeePct(req.minFeePct());
        if (req.maxFeePct() != null) {
            profile.setMaxFeePct(Math.min(req.maxFeePct(), 20));
        }
        if (req.maxListings() != null) profile.setMaxListings(req.maxListings());

        return profileRepo.save(profile);
    }

    @Transactional
    public CohostAgreement createAgreement(UUID hostId, UUID listingId, UUID cohostId,
                                            CohostAgreementRequest req) {
        CohostProfile cohostProfile = profileRepo.findByHostId(cohostId)
                .orElseThrow(() -> new NoSuchElementException("Cohost profile not found: " + cohostId));

        if (!cohostProfile.getVerified()) {
            throw new IllegalStateException("Cohost is not verified: " + cohostId);
        }

        if (cohostProfile.getCurrentListings() >= cohostProfile.getMaxListings()) {
            throw new IllegalStateException("Cohost is at capacity: " + cohostId);
        }

        if (req.feePct() < cohostProfile.getMinFeePct() || req.feePct() > cohostProfile.getMaxFeePct()) {
            throw new IllegalArgumentException("Fee " + req.feePct()
                    + "% is outside cohost's accepted range ["
                    + cohostProfile.getMinFeePct() + "%, "
                    + cohostProfile.getMaxFeePct() + "%]");
        }

        CohostAgreement agreement = CohostAgreement.builder()
                .listingId(listingId)
                .hostId(hostId)
                .cohostId(cohostId)
                .feePct(req.feePct())
                .services(req.services())
                .startDate(req.startDate())
                .build();

        return agreementRepo.save(agreement);
    }

    @Transactional
    public CohostAgreement acceptAgreement(UUID cohostId, UUID agreementId) {
        CohostAgreement agreement = agreementRepo.findById(agreementId)
                .orElseThrow(() -> new NoSuchElementException("Agreement not found: " + agreementId));

        if (!agreement.getCohostId().equals(cohostId)) {
            throw new IllegalArgumentException("Agreement does not belong to this cohost");
        }

        agreement.setStatus(AgreementStatus.ACTIVE);

        CohostProfile profile = profileRepo.findByHostId(cohostId)
                .orElseThrow(() -> new NoSuchElementException("Cohost profile not found: " + cohostId));
        profile.setCurrentListings(profile.getCurrentListings() + 1);
        profileRepo.save(profile);

        return agreementRepo.save(agreement);
    }

    public Page<CohostProfile> searchCohosts(String city, Pageable pageable) {
        return profileRepo.findActiveCohostsByCity(city, pageable);
    }

    public List<CohostEarnings> getEarnings(UUID hostId) {
        List<CohostAgreement> agreements = agreementRepo.findByHostIdOrCohostId(hostId, hostId);
        List<UUID> agreementIds = agreements.stream().map(CohostAgreement::getId).toList();
        if (agreementIds.isEmpty()) {
            return List.of();
        }
        return earningsRepo.findByAgreementIdIn(agreementIds);
    }

    @Transactional
    public CohostProfile verifyProfile(UUID profileId) {
        CohostProfile profile = profileRepo.findById(profileId)
                .orElseThrow(() -> new NoSuchElementException("Cohost profile not found: " + profileId));
        profile.setVerified(true);
        return profileRepo.save(profile);
    }
}
