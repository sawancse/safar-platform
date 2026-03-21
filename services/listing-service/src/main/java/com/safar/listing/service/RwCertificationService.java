package com.safar.listing.service;

import com.safar.listing.dto.RwCertRequest;
import com.safar.listing.dto.RwCertResponse;
import com.safar.listing.entity.Listing;
import com.safar.listing.entity.RwCertification;
import com.safar.listing.entity.enums.RwCertStatus;
import com.safar.listing.repository.ListingRepository;
import com.safar.listing.repository.RwCertificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RwCertificationService {

    private final RwCertificationRepository rwCertificationRepository;
    private final ListingRepository listingRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public RwCertResponse apply(UUID hostId, UUID listingId, RwCertRequest req) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + listingId));
        if (!listing.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("Listing does not belong to this host");
        }

        RwCertification cert = RwCertification.builder()
                .listingId(listingId)
                .wifiSpeedMbps(req.wifiSpeedMbps())
                .hasDedicatedDesk(req.hasDedicatedDesk() != null && req.hasDedicatedDesk())
                .hasPowerBackup(req.hasPowerBackup() != null && req.hasPowerBackup())
                .quietHoursFrom(req.quietHoursFrom())
                .quietHoursTo(req.quietHoursTo())
                .additionalNotes(req.additionalNotes())
                .build();

        // Auto-approve if wifi >= 50 AND dedicated desk AND power backup
        boolean autoApprove = req.wifiSpeedMbps() != null && req.wifiSpeedMbps() >= 50
                && Boolean.TRUE.equals(req.hasDedicatedDesk())
                && Boolean.TRUE.equals(req.hasPowerBackup());

        if (autoApprove) {
            cert.setStatus(RwCertStatus.CERTIFIED);
            cert.setCertifiedAt(OffsetDateTime.now());
            cert.setExpiresAt(OffsetDateTime.now().plusYears(1));

            listing.setRwCertified(true);
            listing.setRwCertifiedAt(OffsetDateTime.now());
            listingRepository.save(listing);

            String payload = String.format("{\"listingId\":\"%s\",\"hostId\":\"%s\"}",
                    listingId, hostId);
            kafkaTemplate.send("listing.rw_certified", listingId.toString(), payload);
            log.info("Listing {} auto-approved for remote work certification", listingId);
        } else {
            cert.setStatus(RwCertStatus.PENDING);
            log.info("Listing {} RW certification submitted as PENDING", listingId);
        }

        RwCertification saved = rwCertificationRepository.save(cert);
        return toResponse(saved);
    }

    @Transactional
    public RwCertResponse adminReview(UUID certId, boolean approve, String adminNote) {
        RwCertification cert = rwCertificationRepository.findById(certId)
                .orElseThrow(() -> new NoSuchElementException("Certification not found: " + certId));

        if (approve) {
            cert.setStatus(RwCertStatus.CERTIFIED);
            cert.setCertifiedAt(OffsetDateTime.now());
            cert.setExpiresAt(OffsetDateTime.now().plusYears(1));

            Listing listing = listingRepository.findById(cert.getListingId())
                    .orElseThrow(() -> new NoSuchElementException("Listing not found: " + cert.getListingId()));
            listing.setRwCertified(true);
            listing.setRwCertifiedAt(OffsetDateTime.now());
            listingRepository.save(listing);

            String payload = String.format("{\"listingId\":\"%s\"}", cert.getListingId());
            kafkaTemplate.send("listing.rw_certified", cert.getListingId().toString(), payload);
            log.info("Certification {} approved by admin for listing {}", certId, cert.getListingId());
        } else {
            cert.setStatus(RwCertStatus.REJECTED);
            log.info("Certification {} rejected by admin for listing {}", certId, cert.getListingId());
        }

        cert.setAdminNote(adminNote);
        RwCertification saved = rwCertificationRepository.save(cert);
        return toResponse(saved);
    }

    public RwCertResponse getByListingId(UUID listingId) {
        RwCertification cert = rwCertificationRepository.findByListingId(listingId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No RW certification found for listing: " + listingId));
        return toResponse(cert);
    }

    public Page<RwCertResponse> getPending(Pageable pageable) {
        return rwCertificationRepository.findByStatus(RwCertStatus.PENDING, pageable)
                .map(this::toResponse);
    }

    private RwCertResponse toResponse(RwCertification c) {
        return new RwCertResponse(
                c.getId(), c.getListingId(), c.getStatus(),
                c.getWifiSpeedMbps(), c.getHasDedicatedDesk(), c.getHasPowerBackup(),
                c.getQuietHoursFrom(), c.getQuietHoursTo(), c.getAdditionalNotes(),
                c.getCertifiedAt(), c.getExpiresAt(), c.getSubmittedAt(),
                c.getAdminNote()
        );
    }
}
