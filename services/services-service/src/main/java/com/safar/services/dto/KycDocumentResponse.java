package com.safar.services.dto;

import com.safar.services.entity.VendorKycDocument;
import com.safar.services.entity.enums.KycDocumentType;
import com.safar.services.entity.enums.KycVerificationStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record KycDocumentResponse(
        UUID id,
        UUID serviceListingId,
        KycDocumentType documentType,
        String documentUrl,
        String documentNumber,
        KycVerificationStatus verificationStatus,
        OffsetDateTime verifiedAt,
        UUID verifiedBy,
        LocalDate expiresAt,
        String rejectionReason,
        OffsetDateTime uploadedAt
) {
    public static KycDocumentResponse from(VendorKycDocument d) {
        return new KycDocumentResponse(
                d.getId(), d.getServiceListingId(), d.getDocumentType(),
                d.getDocumentUrl(), d.getDocumentNumber(), d.getVerificationStatus(),
                d.getVerifiedAt(), d.getVerifiedBy(), d.getExpiresAt(),
                d.getRejectionReason(), d.getUploadedAt()
        );
    }
}
