package com.safar.user.entity;

import com.safar.user.entity.enums.KycStatus;
import com.safar.user.security.EncryptedFieldConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "host_kyc", schema = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HostKyc {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private KycStatus status = KycStatus.NOT_STARTED;

    // Step 1: Identity
    @Column(name = "full_legal_name")
    private String fullLegalName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "aadhaar_number")
    @Convert(converter = EncryptedFieldConverter.class)
    private String aadhaarNumber;

    @Column(name = "aadhaar_verified")
    @Builder.Default
    private Boolean aadhaarVerified = false;

    @Column(name = "pan_number")
    @Convert(converter = EncryptedFieldConverter.class)
    private String panNumber;

    @Column(name = "pan_verified")
    @Builder.Default
    private Boolean panVerified = false;

    // Step 2: Address
    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    private String city;
    private String state;
    private String pincode;

    // Step 3: Bank details
    @Column(name = "bank_account_name")
    private String bankAccountName;

    @Column(name = "bank_account_number")
    @Convert(converter = EncryptedFieldConverter.class)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc")
    @Convert(converter = EncryptedFieldConverter.class)
    private String bankIfsc;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_verified")
    @Builder.Default
    private Boolean bankVerified = false;

    // Step 4: Business (optional)
    @Column(length = 15)
    private String gstin;

    @Column(name = "gst_verified")
    @Builder.Default
    private Boolean gstVerified = false;

    @Column(name = "business_name")
    private String businessName;

    @Column(name = "business_type", length = 50)
    private String businessType;

    // Meta
    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
