package com.safar.listing.service;

import com.safar.listing.dto.VpnEnrollRequest;
import com.safar.listing.entity.VpnListing;
import com.safar.listing.entity.VpnReferral;
import com.safar.listing.repository.VpnListingRepository;
import com.safar.listing.repository.VpnReferralRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VacantPropertyNetworkService {

    private final VpnListingRepository vpnListingRepository;
    private final VpnReferralRepository vpnReferralRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final int MAX_COMMISSION_PCT = 20;

    @Transactional
    public VpnListing enrollInNetwork(UUID hostId, UUID listingId, VpnEnrollRequest req) {
        int commissionPct = req.commissionPct() != null
                ? Math.min(req.commissionPct(), MAX_COMMISSION_PCT)
                : 10;

        VpnListing vpnListing = vpnListingRepository.findByListingId(listingId)
                .map(existing -> {
                    existing.setCommissionPct(commissionPct);
                    existing.setOpenToNetwork(true);
                    existing.setMinStayNights(req.minStayNights() != null ? req.minStayNights() : 1);
                    existing.setAvailableFrom(req.availableFrom());
                    existing.setAvailableTo(req.availableTo());
                    return existing;
                })
                .orElseGet(() -> VpnListing.builder()
                        .listingId(listingId)
                        .hostId(hostId)
                        .commissionPct(commissionPct)
                        .openToNetwork(true)
                        .minStayNights(req.minStayNights() != null ? req.minStayNights() : 1)
                        .availableFrom(req.availableFrom())
                        .availableTo(req.availableTo())
                        .build());

        VpnListing saved = vpnListingRepository.save(vpnListing);

        String payload = String.format(
                "{\"listingId\":\"%s\",\"hostId\":\"%s\",\"commissionPct\":%d}",
                listingId, hostId, commissionPct);
        kafkaTemplate.send("vpn.listing.enrolled", listingId.toString(), payload);
        log.info("Listing {} enrolled in VPN by host {}", listingId, hostId);

        return saved;
    }

    @Transactional
    public void removeFromNetwork(UUID hostId, UUID listingId) {
        VpnListing vpnListing = vpnListingRepository.findByListingId(listingId)
                .orElseThrow(() -> new NoSuchElementException("VPN listing not found for listing: " + listingId));

        if (!vpnListing.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("VPN listing does not belong to this host");
        }

        vpnListing.setOpenToNetwork(false);
        vpnListingRepository.save(vpnListing);
        log.info("Listing {} removed from VPN by host {}", listingId, hostId);
    }

    public String generateReferralLink(UUID referrerId, UUID listingId) {
        VpnReferral referral = VpnReferral.builder()
                .referrerId(referrerId)
                .listingId(listingId)
                .status("PENDING")
                .build();
        vpnReferralRepository.save(referral);

        String referrerPart = referrerId.toString().substring(0, 6);
        String listingPart = listingId.toString().substring(0, 4);
        String code = "VPN-" + referrerPart + "-" + listingPart;
        log.info("Referral link generated: {} for referrer {} on listing {}", code, referrerId, listingId);
        return code;
    }

    public Page<VpnListing> findNetworkListings(String city, Pageable pageable) {
        return vpnListingRepository.findNetworkListings(city, pageable);
    }

    public Page<VpnReferral> getReferralHistory(UUID referrerId, Pageable pageable) {
        return vpnReferralRepository.findByReferrerId(referrerId, pageable);
    }
}
