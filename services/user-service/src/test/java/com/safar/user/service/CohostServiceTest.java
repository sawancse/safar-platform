package com.safar.user.service;

import com.safar.user.dto.CohostAgreementRequest;
import com.safar.user.dto.CohostProfileRequest;
import com.safar.user.entity.CohostProfile;
import com.safar.user.repository.CohostAgreementRepository;
import com.safar.user.repository.CohostEarningsRepository;
import com.safar.user.repository.CohostProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CohostServiceTest {

    @Mock CohostProfileRepository profileRepo;
    @Mock CohostAgreementRepository agreementRepo;
    @Mock CohostEarningsRepository earningsRepo;
    @InjectMocks CohostService cohostService;

    private final UUID HOST_ID = UUID.randomUUID();
    private final UUID COHOST_ID = UUID.randomUUID();
    private final UUID LISTING_ID = UUID.randomUUID();

    @Test
    void createProfile_duplicateRejected() {
        when(profileRepo.existsByHostId(HOST_ID)).thenReturn(true);

        CohostProfileRequest req = new CohostProfileRequest(
                "Bio", "cleaning,checkin", "Mumbai,Goa", 5, 15, 5);

        assertThatThrownBy(() -> cohostService.createProfile(HOST_ID, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createAgreement_unverifiedCohostRejected() {
        CohostProfile unverified = CohostProfile.builder()
                .id(UUID.randomUUID())
                .hostId(COHOST_ID)
                .verified(false)
                .currentListings(0)
                .maxListings(5)
                .minFeePct(5)
                .maxFeePct(15)
                .build();
        when(profileRepo.findByHostId(COHOST_ID)).thenReturn(Optional.of(unverified));

        CohostAgreementRequest req = new CohostAgreementRequest(10, "cleaning", LocalDate.now());

        assertThatThrownBy(() -> cohostService.createAgreement(HOST_ID, LISTING_ID, COHOST_ID, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not verified");
    }

    @Test
    void createAgreement_feeOutsideRangeRejected() {
        CohostProfile verified = CohostProfile.builder()
                .id(UUID.randomUUID())
                .hostId(COHOST_ID)
                .verified(true)
                .currentListings(0)
                .maxListings(5)
                .minFeePct(5)
                .maxFeePct(15)
                .build();
        when(profileRepo.findByHostId(COHOST_ID)).thenReturn(Optional.of(verified));

        CohostAgreementRequest req = new CohostAgreementRequest(25, "cleaning", LocalDate.now());

        assertThatThrownBy(() -> cohostService.createAgreement(HOST_ID, LISTING_ID, COHOST_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside cohost's accepted range");
    }
}
