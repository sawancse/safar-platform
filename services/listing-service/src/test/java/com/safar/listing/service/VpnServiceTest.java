package com.safar.listing.service;

import com.safar.listing.dto.VpnEnrollRequest;
import com.safar.listing.entity.VpnListing;
import com.safar.listing.entity.VpnReferral;
import com.safar.listing.repository.VpnListingRepository;
import com.safar.listing.repository.VpnReferralRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VpnServiceTest {

    @Mock
    VpnListingRepository vpnListingRepository;
    @Mock
    VpnReferralRepository vpnReferralRepository;
    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    VacantPropertyNetworkService vpnService;

    private final UUID hostId = UUID.randomUUID();
    private final UUID listingId = UUID.randomUUID();

    @Test
    void enrollInNetwork_capsCommissionAt20() {
        VpnEnrollRequest req = new VpnEnrollRequest(
                25, 2, LocalDate.now(), LocalDate.now().plusDays(30));

        when(vpnListingRepository.findByListingId(listingId)).thenReturn(Optional.empty());
        when(vpnListingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VpnListing result = vpnService.enrollInNetwork(hostId, listingId, req);

        assertThat(result.getCommissionPct()).isEqualTo(20);
        assertThat(result.getOpenToNetwork()).isTrue();
        verify(kafkaTemplate).send(eq("vpn.listing.enrolled"), eq(listingId.toString()), any());
    }

    @Test
    void generateReferralLink_formatCorrect() {
        when(vpnReferralRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID referrerId = UUID.fromString("abcdef12-3456-7890-abcd-ef1234567890");
        UUID refListingId = UUID.fromString("12345678-abcd-ef12-3456-7890abcdef12");

        String code = vpnService.generateReferralLink(referrerId, refListingId);

        assertThat(code).isEqualTo("VPN-abcdef-1234");
        assertThat(code).startsWith("VPN-");
        assertThat(code.split("-")).hasSize(3);
    }

    @Test
    void removeFromNetwork_setsOpenToNetworkFalse() {
        VpnListing existing = VpnListing.builder()
                .id(UUID.randomUUID())
                .listingId(listingId)
                .hostId(hostId)
                .commissionPct(15)
                .openToNetwork(true)
                .minStayNights(1)
                .build();

        when(vpnListingRepository.findByListingId(listingId)).thenReturn(Optional.of(existing));
        when(vpnListingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        vpnService.removeFromNetwork(hostId, listingId);

        assertThat(existing.getOpenToNetwork()).isFalse();
        verify(vpnListingRepository).save(existing);
    }
}
