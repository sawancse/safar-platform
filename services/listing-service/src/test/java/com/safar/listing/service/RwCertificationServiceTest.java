package com.safar.listing.service;

import com.safar.listing.dto.RwCertRequest;
import com.safar.listing.dto.RwCertResponse;
import com.safar.listing.entity.Listing;
import com.safar.listing.entity.RwCertification;
import com.safar.listing.entity.enums.ListingStatus;
import com.safar.listing.entity.enums.ListingType;
import com.safar.listing.entity.enums.PricingUnit;
import com.safar.listing.entity.enums.RwCertStatus;
import com.safar.listing.repository.ListingRepository;
import com.safar.listing.repository.RwCertificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RwCertificationServiceTest {

    @Mock
    RwCertificationRepository rwCertificationRepository;
    @Mock
    ListingRepository listingRepository;
    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    RwCertificationService rwCertificationService;

    private final UUID hostId = UUID.randomUUID();
    private final UUID listingId = UUID.randomUUID();

    private Listing buildListing() {
        return Listing.builder()
                .id(listingId).hostId(hostId)
                .title("Test").description("desc")
                .type(ListingType.HOME)
                .status(ListingStatus.VERIFIED)
                .basePricePaise(500000L).maxGuests(4)
                .addressLine1("addr").city("Mumbai").state("MH").pincode("400001")
                .lat(BigDecimal.ZERO).lng(BigDecimal.ZERO)
                .pricingUnit(PricingUnit.NIGHT).minBookingHours(1)
                .instantBook(false).gstApplicable(true)
                .aiPricingEnabled(false)
                .build();
    }

    @Test
    void apply_allCriteriaMet_autoApproves() {
        Listing listing = buildListing();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(rwCertificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RwCertRequest req = new RwCertRequest(100, true, true, null, null, null);

        RwCertResponse response = rwCertificationService.apply(hostId, listingId, req);

        assertThat(response.status()).isEqualTo(RwCertStatus.CERTIFIED);
        assertThat(response.certifiedAt()).isNotNull();
        verify(kafkaTemplate).send(eq("listing.rw_certified"), eq(listingId.toString()), any());
        assertThat(listing.getRwCertified()).isTrue();
    }

    @Test
    void apply_wifiTooSlow_pendingStatus() {
        Listing listing = buildListing();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(rwCertificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RwCertRequest req = new RwCertRequest(30, true, true, null, null, null);

        RwCertResponse response = rwCertificationService.apply(hostId, listingId, req);

        assertThat(response.status()).isEqualTo(RwCertStatus.PENDING);
        assertThat(response.certifiedAt()).isNull();
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void adminReview_approve_certifiesAndPublishesEvent() {
        UUID certId = UUID.randomUUID();
        RwCertification cert = RwCertification.builder()
                .id(certId)
                .listingId(listingId)
                .status(RwCertStatus.PENDING)
                .wifiSpeedMbps(30)
                .hasDedicatedDesk(true)
                .hasPowerBackup(false)
                .build();
        Listing listing = buildListing();

        when(rwCertificationRepository.findById(certId)).thenReturn(Optional.of(cert));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(rwCertificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RwCertResponse response = rwCertificationService.adminReview(certId, true, "Looks good");

        assertThat(response.status()).isEqualTo(RwCertStatus.CERTIFIED);
        assertThat(response.adminNote()).isEqualTo("Looks good");
        assertThat(listing.getRwCertified()).isTrue();
        verify(kafkaTemplate).send(eq("listing.rw_certified"), eq(listingId.toString()), any());
    }
}
