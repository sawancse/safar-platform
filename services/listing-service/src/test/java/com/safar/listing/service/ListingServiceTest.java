package com.safar.listing.service;

import com.safar.listing.dto.CreateListingRequest;
import com.safar.listing.entity.Listing;
import com.safar.listing.entity.enums.*;
import com.safar.listing.entity.enums.MediaType;
import com.safar.listing.entity.enums.ModerationStatus;
import com.safar.listing.repository.ListingMediaRepository;
import com.safar.listing.repository.ListingRepository;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    ListingRepository listingRepository;
    @Mock
    ListingMediaRepository listingMediaRepository;
    @Mock
    SubscriptionTierClient subscriptionTierClient;
    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    ListingService listingService;

    private final UUID hostId = UUID.randomUUID();
    private final UUID listingId = UUID.randomUUID();

    private CreateListingRequest validRequest() {
        return new CreateListingRequest(
                "Test Villa", "A lovely villa", ListingType.HOME, null,
                "123 MG Road", null, "Mumbai", "Maharashtra", "400001",
                new BigDecimal("19.076090"), new BigDecimal("72.877426"),
                4, 2, 2, null, null, 500000L, PricingUnit.NIGHT, 1,
                false, true, null,
                null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                // PG/Co-living fields
                null, null, null, null, null, null, null, null, null,
                // Insurance
                null, null, null,
                // Hotel fields
                null, null, null, null
        );
    }

    @Test
    void createListing_validHome_returnsDraft() {
        when(subscriptionTierClient.getTier(hostId)).thenReturn(HostTier.STARTER);
        when(listingRepository.findByHostId(hostId)).thenReturn(List.of());
        when(listingRepository.save(any())).thenAnswer(inv -> {
            Listing l = inv.getArgument(0);
            // simulate ID generation
            return l;
        });

        var response = listingService.createListing(hostId, validRequest());

        assertThat(response.status()).isEqualTo(ListingStatus.DRAFT);
        assertThat(response.type()).isEqualTo(ListingType.HOME);
        assertThat(response.hostId()).isEqualTo(hostId);
    }

    @Test
    void createListing_commercialWithoutCategory_throws() {
        var req = new CreateListingRequest(
                "Meeting Space", "A room", ListingType.COMMERCIAL, null,
                "123 Road", null, "Delhi", "Delhi", "110001",
                BigDecimal.ZERO, BigDecimal.ZERO,
                10, null, null, null, null, 100000L, PricingUnit.HOUR, 1,
                false, true, null,
                null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                // PG/Co-living fields
                null, null, null, null, null, null, null, null, null,
                // Insurance
                null, null, null,
                // Hotel fields
                null, null, null, null
        );
        assertThatThrownBy(() -> listingService.createListing(hostId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("commercialCategory");
    }

    @Test
    void createListing_commercialWithCategory_succeeds() {
        var req = new CreateListingRequest(
                "Meeting Room", "Great space", ListingType.COMMERCIAL, CommercialCategory.MEETING_ROOM,
                "123 Road", null, "Delhi", "Delhi", "110001",
                BigDecimal.valueOf(28.6139), BigDecimal.valueOf(77.2090),
                10, null, null, null, null, 100000L, PricingUnit.HOUR, 1,
                false, true, null,
                null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                // PG/Co-living fields
                null, null, null, null, null, null, null, null, null,
                // Insurance
                null, null, null,
                // Hotel fields
                null, null, null, null
        );
        when(subscriptionTierClient.getTier(hostId)).thenReturn(HostTier.COMMERCIAL);
        when(listingRepository.findByHostId(hostId)).thenReturn(List.of());
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = listingService.createListing(hostId, req);

        assertThat(response.commercialCategory()).isEqualTo(CommercialCategory.MEETING_ROOM);
    }

    @Test
    void getListing_found_returnsListing() {
        Listing l = Listing.builder()
                .id(listingId).hostId(hostId)
                .title("Villa").description("desc")
                .type(ListingType.HOME)
                .status(ListingStatus.DRAFT)
                .basePricePaise(500000L)
                .maxGuests(4)
                .addressLine1("addr").city("Mumbai").state("MH").pincode("400001")
                .lat(BigDecimal.ZERO).lng(BigDecimal.ZERO)
                .pricingUnit(PricingUnit.NIGHT).minBookingHours(1)
                .instantBook(false).gstApplicable(true)
                .aiPricingEnabled(false)
                .build();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(l));

        var response = listingService.getListing(listingId);

        assertThat(response.title()).isEqualTo("Villa");
    }

    @Test
    void getListing_notFound_throws() {
        when(listingRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> listingService.getListing(UUID.randomUUID()))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void submitForVerification_draft_changesToPending() {
        Listing l = Listing.builder()
                .id(listingId).hostId(hostId)
                .title("Villa").description("desc")
                .type(ListingType.HOME)
                .status(ListingStatus.DRAFT)
                .basePricePaise(500000L)
                .maxGuests(4)
                .addressLine1("addr").city("Mumbai").state("MH").pincode("400001")
                .lat(BigDecimal.ZERO).lng(BigDecimal.ZERO)
                .pricingUnit(PricingUnit.NIGHT).minBookingHours(1)
                .instantBook(false).gstApplicable(true)
                .aiPricingEnabled(false)
                .build();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(l));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(listingMediaRepository.countByListingIdAndTypeAndModerationStatus(
                listingId, MediaType.PHOTO, ModerationStatus.APPROVED)).thenReturn(2L);
        when(listingMediaRepository.existsByListingIdAndIsPrimaryTrue(listingId)).thenReturn(true);

        var response = listingService.submitForVerification(listingId, hostId);

        assertThat(response.status()).isEqualTo(ListingStatus.PENDING_VERIFICATION);
    }

    @Test
    void submitForVerification_noPhotos_throws() {
        Listing l = Listing.builder()
                .id(listingId).hostId(hostId)
                .title("Villa").description("desc")
                .type(ListingType.HOME)
                .status(ListingStatus.DRAFT)
                .basePricePaise(500000L)
                .maxGuests(4)
                .addressLine1("addr").city("Mumbai").state("MH").pincode("400001")
                .lat(BigDecimal.ZERO).lng(BigDecimal.ZERO)
                .pricingUnit(PricingUnit.NIGHT).minBookingHours(1)
                .instantBook(false).gstApplicable(true)
                .aiPricingEnabled(false)
                .build();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(l));
        when(listingMediaRepository.countByListingIdAndTypeAndModerationStatus(
                listingId, MediaType.PHOTO, ModerationStatus.APPROVED)).thenReturn(0L);

        assertThatThrownBy(() -> listingService.submitForVerification(listingId, hostId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("At least 1 photo is required");
    }

    @Test
    void submitForVerification_noPrimary_throws() {
        Listing l = Listing.builder()
                .id(listingId).hostId(hostId)
                .title("Villa").description("desc")
                .type(ListingType.HOME)
                .status(ListingStatus.DRAFT)
                .basePricePaise(500000L)
                .maxGuests(4)
                .addressLine1("addr").city("Mumbai").state("MH").pincode("400001")
                .lat(BigDecimal.ZERO).lng(BigDecimal.ZERO)
                .pricingUnit(PricingUnit.NIGHT).minBookingHours(1)
                .instantBook(false).gstApplicable(true)
                .aiPricingEnabled(false)
                .build();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(l));
        when(listingMediaRepository.countByListingIdAndTypeAndModerationStatus(
                listingId, MediaType.PHOTO, ModerationStatus.APPROVED)).thenReturn(3L);
        when(listingMediaRepository.existsByListingIdAndIsPrimaryTrue(listingId)).thenReturn(false);

        assertThatThrownBy(() -> listingService.submitForVerification(listingId, hostId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("primary photo must be set");
    }

    @Test
    void submitForVerification_alreadyPending_throws() {
        Listing l = Listing.builder()
                .id(listingId).hostId(hostId)
                .title("x").description("x").type(ListingType.HOME)
                .status(ListingStatus.PENDING_VERIFICATION)
                .basePricePaise(100L).maxGuests(1)
                .addressLine1("a").city("c").state("s").pincode("000000")
                .lat(BigDecimal.ZERO).lng(BigDecimal.ZERO)
                .pricingUnit(PricingUnit.NIGHT).minBookingHours(1)
                .instantBook(false).gstApplicable(true).aiPricingEnabled(false)
                .build();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(l));

        assertThatThrownBy(() -> listingService.submitForVerification(listingId, hostId))
                .isInstanceOf(IllegalStateException.class);
    }
}
