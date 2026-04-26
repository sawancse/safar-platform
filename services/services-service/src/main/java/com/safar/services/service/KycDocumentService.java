package com.safar.services.service;

import com.safar.services.dto.UploadKycDocumentRequest;
import com.safar.services.entity.ServiceListing;
import com.safar.services.entity.VendorKycDocument;
import com.safar.services.entity.enums.KycVerificationStatus;
import com.safar.services.repository.ServiceListingRepository;
import com.safar.services.repository.VendorKycDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * KYC document upload + admin verification.
 *
 * Vendor flow: upload to S3 (via media-service) -> POST URL + doc-type to us
 * -> we store row in PENDING. Validator accepts PENDING for the publish gate.
 *
 * Admin flow: see queue of docs, verify (-> VERIFIED) or reject (-> REJECTED
 * with reason). Rejected docs no longer satisfy the publish gate.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KycDocumentService {

    private final VendorKycDocumentRepository kycRepo;
    private final ServiceListingRepository listingRepo;

    @Transactional
    public VendorKycDocument upload(UUID listingId, UploadKycDocumentRequest req, UUID vendorUserId) {
        ServiceListing listing = listingRepo.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + listingId));
        if (!listing.getVendorUserId().equals(vendorUserId)) {
            throw new AccessDeniedException("Listing belongs to another vendor");
        }
        if (req.documentType() == null) throw new IllegalArgumentException("documentType required");
        if (req.documentUrl() == null || req.documentUrl().isBlank())
            throw new IllegalArgumentException("documentUrl required");

        // Replace any existing doc of the same type — vendor reuploads override
        kycRepo.findByServiceListingIdAndDocumentType(listingId, req.documentType())
                .forEach(existing -> kycRepo.deleteById(existing.getId()));

        VendorKycDocument doc = VendorKycDocument.builder()
                .serviceListingId(listingId)
                .documentType(req.documentType())
                .documentUrl(req.documentUrl())
                .documentNumber(req.documentNumber())
                .verificationStatus(KycVerificationStatus.PENDING)
                .expiresAt(req.expiresAt())
                .build();

        VendorKycDocument saved = kycRepo.save(doc);
        log.info("KYC doc uploaded: listing={} type={} status=PENDING", listingId, req.documentType());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<VendorKycDocument> listForListing(UUID listingId, UUID requesterUserId, boolean isAdmin) {
        ServiceListing listing = listingRepo.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + listingId));
        if (!isAdmin && !listing.getVendorUserId().equals(requesterUserId)) {
            throw new AccessDeniedException("Cannot view another vendor's KYC docs");
        }
        return kycRepo.findByServiceListingId(listingId);
    }

    @Transactional
    public VendorKycDocument verify(UUID docId, UUID adminUserId) {
        VendorKycDocument doc = kycRepo.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Doc not found: " + docId));
        doc.setVerificationStatus(KycVerificationStatus.VERIFIED);
        doc.setVerifiedAt(OffsetDateTime.now());
        doc.setVerifiedBy(adminUserId);
        doc.setRejectionReason(null);
        return kycRepo.save(doc);
    }

    @Transactional
    public VendorKycDocument reject(UUID docId, UUID adminUserId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason required");
        }
        VendorKycDocument doc = kycRepo.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Doc not found: " + docId));
        doc.setVerificationStatus(KycVerificationStatus.REJECTED);
        doc.setVerifiedAt(OffsetDateTime.now());
        doc.setVerifiedBy(adminUserId);
        doc.setRejectionReason(reason);
        return kycRepo.save(doc);
    }
}
