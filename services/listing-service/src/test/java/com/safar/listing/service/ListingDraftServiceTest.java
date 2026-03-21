package com.safar.listing.service;

import com.safar.listing.dto.ListingResponse;
import com.safar.listing.entity.ListingDraft;
import com.safar.listing.entity.enums.DraftStatus;
import com.safar.listing.entity.enums.ListingStatus;
import com.safar.listing.entity.enums.ListingType;
import com.safar.listing.entity.enums.PricingUnit;
import com.safar.listing.repository.ListingDraftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingDraftServiceTest {

    @Mock
    ListingDraftRepository draftRepository;

    @Mock
    ListingService listingService;

    @InjectMocks
    ListingDraftService listingDraftService;

    private final UUID hostId = UUID.randomUUID();

    @Test
    void generateDraft_createsDraftWithTemplateValues() {
        when(draftRepository.save(any(ListingDraft.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ListingDraft draft = listingDraftService.generateDraft(
                hostId, "123 MG Road, Mumbai, Maharashtra", "HOME");

        assertThat(draft.getHostId()).isEqualTo(hostId);
        assertThat(draft.getAiTitle()).contains("Mumbai");
        assertThat(draft.getAiDescription()).isNotBlank();
        assertThat(draft.getAiAmenities()).contains("wifi");
        assertThat(draft.getAiSuggestedPricePaise()).isGreaterThan(0);
        assertThat(draft.getStatus()).isEqualTo(DraftStatus.DRAFT);
        verify(draftRepository).save(any(ListingDraft.class));
    }

    @Test
    void convertToListing_createsListingAndSetsDraftConverted() {
        UUID draftId = UUID.randomUUID();
        ListingDraft draft = ListingDraft.builder()
                .id(draftId)
                .hostId(hostId)
                .address("123 MG Road, Mumbai, Maharashtra")
                .type("HOME")
                .aiTitle("Cozy Home in Mumbai")
                .aiDescription("A beautiful home in Mumbai")
                .aiAmenities("wifi,ac,kitchen")
                .aiSuggestedPricePaise(350000L)
                .status(DraftStatus.DRAFT)
                .build();

        ListingResponse expectedResponse = new ListingResponse(
                UUID.randomUUID(), hostId,
                "Cozy Home in Mumbai", "A beautiful home in Mumbai",
                ListingType.HOME, null,
                "123 MG Road, Mumbai, Maharashtra", null,
                "Mumbai", "TBD", "000000",
                null, null,
                2, null, null, null,
                null, 350000L, PricingUnit.NIGHT, 1,
                false, ListingStatus.DRAFT, null,
                true, null, false, 0, 0.0, 0, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null,
                null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null,
                null, null, null,
                null, null, // visibilityBoostPercent, preferredPartner
                // PG/Co-living fields
                null, null, null, null, null,
                // Hotel fields
                null, null, null, null,
                // Hotel enhancements
                null, null, null, null, null, null,
                null, null
        );

        when(draftRepository.findById(draftId)).thenReturn(Optional.of(draft));
        when(listingService.createListing(eq(hostId), any())).thenReturn(expectedResponse);
        when(draftRepository.save(any(ListingDraft.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ListingResponse result = listingDraftService.convertToListing(hostId, draftId);

        assertThat(result.title()).isEqualTo("Cozy Home in Mumbai");
        assertThat(draft.getStatus()).isEqualTo(DraftStatus.CONVERTED);
        verify(listingService).createListing(eq(hostId), any());
        verify(draftRepository).save(draft);
    }

    @Test
    void convertToListing_nonOwnedDraft_throwsException() {
        UUID draftId = UUID.randomUUID();
        UUID otherHostId = UUID.randomUUID();

        ListingDraft draft = ListingDraft.builder()
                .id(draftId)
                .hostId(otherHostId)
                .address("456 Park Street, Delhi, Delhi")
                .type("ROOM")
                .aiTitle("Private Room in Delhi")
                .aiDescription("A room in Delhi")
                .aiAmenities("wifi,ac")
                .aiSuggestedPricePaise(200000L)
                .status(DraftStatus.DRAFT)
                .build();

        when(draftRepository.findById(draftId)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> listingDraftService.convertToListing(hostId, draftId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to this host");

        verify(listingService, never()).createListing(any(), any());
    }
}
