package com.safar.chef.entity;

import com.safar.chef.entity.enums.KycDocumentType;
import com.safar.chef.entity.enums.KycVerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vendor_kyc_documents", schema = "services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorKycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_listing_id", nullable = false)
    private UUID serviceListingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private KycDocumentType documentType;

    @Column(name = "document_url", nullable = false, columnDefinition = "TEXT")
    private String documentUrl;

    @Column(name = "document_number", length = 50)
    private String documentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private KycVerificationStatus verificationStatus;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private OffsetDateTime uploadedAt;
}
