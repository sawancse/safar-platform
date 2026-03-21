package com.safar.listing.service;

import com.safar.listing.dto.AvailabilityRequest;
import com.safar.listing.dto.AvailabilityResponse;
import com.safar.listing.entity.Availability;
import com.safar.listing.repository.AvailabilityRepository;
import com.safar.listing.repository.ListingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock AvailabilityRepository availabilityRepository;
    @Mock ListingRepository listingRepository;
    @InjectMocks AvailabilityService availabilityService;

    private final UUID listingId = UUID.randomUUID();

    @Test
    void upsert_newDate_setsIsAvailableFalse() {
        when(listingRepository.existsById(listingId)).thenReturn(true);
        when(availabilityRepository.findByListingIdAndDate(listingId, LocalDate.of(2025, 12, 25)))
                .thenReturn(Optional.empty());
        when(availabilityRepository.save(any())).thenAnswer(inv -> {
            Availability a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AvailabilityRequest req = new AvailabilityRequest(LocalDate.of(2025, 12, 25), false, null, null);
        AvailabilityResponse resp = availabilityService.upsertAvailability(listingId, req);

        assertThat(resp.isAvailable()).isFalse();
        verify(availabilityRepository).save(any());
    }

    @Test
    void upsert_withPriceOverride_storesOverridePrice() {
        when(listingRepository.existsById(listingId)).thenReturn(true);
        when(availabilityRepository.findByListingIdAndDate(listingId, LocalDate.of(2025, 10, 20)))
                .thenReturn(Optional.empty());
        when(availabilityRepository.save(any())).thenAnswer(inv -> {
            Availability a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AvailabilityRequest req = new AvailabilityRequest(LocalDate.of(2025, 10, 20), true, 500000L, 2);
        AvailabilityResponse resp = availabilityService.upsertAvailability(listingId, req);

        assertThat(resp.priceOverridePaise()).isEqualTo(500000L);
        assertThat(resp.minStayNights()).isEqualTo(2);
    }

    @Test
    void upsert_existingDate_updatesInPlace() {
        Availability existing = Availability.builder()
                .id(UUID.randomUUID()).listingId(listingId)
                .date(LocalDate.of(2025, 12, 31))
                .isAvailable(true).minStayNights(1).build();

        when(listingRepository.existsById(listingId)).thenReturn(true);
        when(availabilityRepository.findByListingIdAndDate(listingId, LocalDate.of(2025, 12, 31)))
                .thenReturn(Optional.of(existing));
        when(availabilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AvailabilityRequest req = new AvailabilityRequest(LocalDate.of(2025, 12, 31), false, 999900L, 3);
        AvailabilityResponse resp = availabilityService.upsertAvailability(listingId, req);

        assertThat(resp.isAvailable()).isFalse();
        assertThat(resp.priceOverridePaise()).isEqualTo(999900L);
        // Verify save was called once (update, not insert)
        verify(availabilityRepository, times(1)).save(existing);
    }

    @Test
    void getAvailability_returnsCorrectDateRange() {
        LocalDate from = LocalDate.of(2025, 12, 1);
        LocalDate to   = LocalDate.of(2025, 12, 31);

        Availability a1 = Availability.builder()
                .id(UUID.randomUUID()).listingId(listingId)
                .date(LocalDate.of(2025, 12, 10))
                .isAvailable(false).minStayNights(1).build();
        Availability a2 = Availability.builder()
                .id(UUID.randomUUID()).listingId(listingId)
                .date(LocalDate.of(2025, 12, 25))
                .isAvailable(false).minStayNights(3).build();

        when(listingRepository.existsById(listingId)).thenReturn(true);
        when(availabilityRepository.findByListingIdAndDateBetween(listingId, from, to))
                .thenReturn(List.of(a1, a2));

        List<AvailabilityResponse> result = availabilityService.getAvailability(listingId, from, to);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2025, 12, 10));
        assertThat(result.get(1).date()).isEqualTo(LocalDate.of(2025, 12, 25));
    }

    @Test
    void getAvailability_listingNotFound_throws() {
        when(listingRepository.existsById(listingId)).thenReturn(false);

        assertThatThrownBy(() -> availabilityService.getAvailability(
                listingId, LocalDate.now(), LocalDate.now().plusDays(7)))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("Listing not found");
    }
}
