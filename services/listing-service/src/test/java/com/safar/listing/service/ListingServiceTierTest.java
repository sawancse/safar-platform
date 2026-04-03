package com.safar.listing.service;

import com.safar.listing.dto.CreateListingRequest;
import com.safar.listing.entity.Listing;
import com.safar.listing.entity.enums.*;
import com.safar.listing.repository.ListingMediaRepository;
import com.safar.listing.repository.ListingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingServiceTierTest {

    @Mock ListingRepository listingRepository;
    @Mock ListingMediaRepository listingMediaRepository;
    @Mock SubscriptionTierClient subscriptionTierClient;
    @Mock KafkaTemplate<String, String> kafkaTemplate;
    @InjectMocks ListingService listingService;

    private final UUID hostId = UUID.randomUUID();

    private CreateListingRequest homeRequest() {
        return new CreateListingRequest(
                "Test Home", "A home", ListingType.HOME, null,
                "123 Road", null, "Mumbai", "Maharashtra", "400001",
                BigDecimal.valueOf(19.07), BigDecimal.valueOf(72.87),
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
                null, null, null, null);
    }

    private Listing draftListing() {
        return Listing.builder().hostId(hostId).status(ListingStatus.DRAFT)
                .title("x").description("x").type(ListingType.HOME)
                .basePricePaise(100L).maxGuests(1)
                .addressLine1("a").city("c").state("s").pincode("000000")
                .lat(BigDecimal.ZERO).lng(BigDecimal.ZERO)
                .pricingUnit(PricingUnit.NIGHT).minBookingHours(1)
                .instantBook(false).gstApplicable(true).aiPricingEnabled(false).build();
    }

    @Test
    void createListing_starterUnderLimit_succeeds() {
        when(subscriptionTierClient.getTier(hostId)).thenReturn(HostTier.STARTER);
        when(listingRepository.findByHostId(hostId)).thenReturn(List.of()); // 0 listings
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = listingService.createListing(hostId, homeRequest());

        assertThat(response.status()).isEqualTo(ListingStatus.DRAFT);
    }

    @Test
    void createListing_starterAtLimit_throws() {
        // STARTER = max 2; existing = 2 non-draft listings
        Listing l1 = draftListing(); l1.setStatus(ListingStatus.VERIFIED);
        Listing l2 = draftListing(); l2.setStatus(ListingStatus.VERIFIED);
        when(subscriptionTierClient.getTier(hostId)).thenReturn(HostTier.STARTER);
        when(listingRepository.findByHostId(hostId)).thenReturn(List.of(l1, l2));

        assertThatThrownBy(() -> listingService.createListing(hostId, homeRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Listing limit reached");
    }

    @Test
    void createListing_commercialWithNonCommercialTier_throws() {
        when(subscriptionTierClient.getTier(hostId)).thenReturn(HostTier.STARTER);

        var req = new CreateListingRequest(
                "Meeting Room", "desc", ListingType.COMMERCIAL, CommercialCategory.MEETING_ROOM,
                "123 Road", null, "Delhi", "Delhi", "110001",
                BigDecimal.valueOf(28.61), BigDecimal.valueOf(77.20),
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
                null, null, null, null);

        assertThatThrownBy(() -> listingService.createListing(hostId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Commercial subscription");
    }

    @Test
    void createListing_commercialWithCommercialTier_succeedsWithHourPricing() {
        when(subscriptionTierClient.getTier(hostId)).thenReturn(HostTier.COMMERCIAL);
        when(listingRepository.findByHostId(hostId)).thenReturn(List.of());
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new CreateListingRequest(
                "Meeting Room", "desc", ListingType.COMMERCIAL, CommercialCategory.MEETING_ROOM,
                "123 Road", null, "Delhi", "Delhi", "110001",
                BigDecimal.valueOf(28.61), BigDecimal.valueOf(77.20),
                10, null, null, null, null, 100000L, PricingUnit.NIGHT, 1, // NIGHT ignored, forced to HOUR
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
                null, null, null, null);

        var response = listingService.createListing(hostId, req);

        assertThat(response.pricingUnit()).isEqualTo(PricingUnit.HOUR);
    }
}
