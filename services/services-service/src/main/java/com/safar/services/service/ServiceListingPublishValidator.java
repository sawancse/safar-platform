package com.safar.services.service;

import com.safar.services.config.KycGatesConfig;
import com.safar.services.entity.ServiceListing;
import com.safar.services.entity.VendorKycDocument;
import com.safar.services.entity.enums.KycDocumentType;
import com.safar.services.entity.enums.KycVerificationStatus;
import com.safar.services.entity.enums.ServiceListingType;
import com.safar.services.repository.VendorKycDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gates DRAFT -> PENDING_REVIEW transition on KYC completeness.
 *
 * For each service type, every {@link KycGatesConfig#requiredFor required}
 * document must be uploaded (status PENDING or VERIFIED — REJECTED docs
 * don't count). Missing or rejected docs throw a list of
 * {@link MissingKycException missing types} that the wizard surfaces back
 * to the vendor.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceListingPublishValidator {

    private final KycGatesConfig kycGates;
    private final VendorKycDocumentRepository kycRepo;

    public void validateOrThrow(ServiceListing listing) {
        ServiceListingType type;
        try {
            type = ServiceListingType.valueOf(listing.getServiceType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown service_type on listing " + listing.getId() + ": " + listing.getServiceType());
        }

        Set<KycDocumentType> required = kycGates.requiredFor(type);
        if (required.isEmpty()) return;

        List<VendorKycDocument> docs = kycRepo.findByServiceListingId(listing.getId());
        Set<KycDocumentType> presentAndAcceptable = new HashSet<>();
        for (VendorKycDocument doc : docs) {
            if (doc.getVerificationStatus() != KycVerificationStatus.REJECTED) {
                presentAndAcceptable.add(doc.getDocumentType());
            }
        }

        Set<KycDocumentType> missing = new HashSet<>(required);
        missing.removeAll(presentAndAcceptable);
        if (!missing.isEmpty()) {
            throw new MissingKycException(type, missing);
        }
    }

    public static class MissingKycException extends RuntimeException {
        private final ServiceListingType serviceType;
        private final Set<KycDocumentType> missing;

        public MissingKycException(ServiceListingType serviceType, Set<KycDocumentType> missing) {
            super("Missing required KYC documents for " + serviceType + ": " + missing);
            this.serviceType = serviceType;
            this.missing = missing;
        }

        public ServiceListingType getServiceType() { return serviceType; }
        public Set<KycDocumentType> getMissing() { return missing; }
    }
}
