package com.safar.listing.service;

import com.safar.listing.dto.InvestmentSignalDto;
import com.safar.listing.entity.Listing;
import com.safar.listing.repository.ListingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentSignalServiceTest {

    @Mock
    ListingRepository listingRepository;

    @InjectMocks
    InvestmentSignalService investmentSignalService;

    private final UUID listingId = UUID.randomUUID();

    @Test
    void computeForListing_returnsValidSignal() {
        Listing listing = Listing.builder()
                .id(listingId)
                .city("Goa")
                .build();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        InvestmentSignalDto signal = investmentSignalService.computeForListing(listingId);

        assertThat(signal.avgMonthlyRevenuePaise()).isEqualTo(450000L);
        assertThat(signal.annualYieldPct()).isEqualTo(9.5);
        assertThat(signal.occupancyRatePct()).isEqualTo(78.5);
        assertThat(signal.demandLevel()).isEqualTo("HIGH");
        assertThat(signal.trend()).isEqualTo("RISING");
        assertThat(signal.confidenceLevel()).isEqualTo("HIGH");
        assertThat(signal.dataPointCount()).isGreaterThan(0);
        assertThat(signal.disclaimer()).isNotBlank();
    }
}
