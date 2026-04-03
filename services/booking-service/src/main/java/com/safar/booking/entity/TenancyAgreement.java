package com.safar.booking.entity;

import com.safar.booking.entity.enums.AgreementStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenancy_agreements", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenancyAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenancy_id", nullable = false)
    private UUID tenancyId;

    @Column(name = "agreement_number", nullable = false, unique = true, length = 30)
    private String agreementNumber;

    @Column(name = "tenant_name", nullable = false, length = 200)
    private String tenantName;

    @Column(name = "tenant_phone", length = 20)
    private String tenantPhone;

    @Column(name = "tenant_email", length = 200)
    private String tenantEmail;

    @Column(name = "tenant_aadhaar_last4", length = 4)
    private String tenantAadhaarLast4;

    @Column(name = "host_name", nullable = false, length = 200)
    private String hostName;

    @Column(name = "host_phone", length = 20)
    private String hostPhone;

    @Column(name = "property_address", nullable = false, columnDefinition = "TEXT")
    private String propertyAddress;

    @Column(name = "room_description", length = 500)
    private String roomDescription;

    @Column(name = "move_in_date", nullable = false)
    private LocalDate moveInDate;

    @Column(name = "lock_in_period_months", nullable = false)
    @Builder.Default
    private int lockInPeriodMonths = 0;

    @Column(name = "notice_period_days", nullable = false)
    private int noticePeriodDays;

    @Column(name = "monthly_rent_paise", nullable = false)
    private long monthlyRentPaise;

    @Column(name = "security_deposit_paise", nullable = false)
    private long securityDepositPaise;

    @Column(name = "maintenance_charges_paise", nullable = false)
    @Builder.Default
    private long maintenanceChargesPaise = 0;

    @Column(name = "agreement_text", nullable = false, columnDefinition = "TEXT")
    private String agreementText;

    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private AgreementStatus status = AgreementStatus.DRAFT;

    @Column(name = "host_signed_at")
    private OffsetDateTime hostSignedAt;

    @Column(name = "host_signature_ip", length = 45)
    private String hostSignatureIp;

    @Column(name = "tenant_signed_at")
    private OffsetDateTime tenantSignedAt;

    @Column(name = "tenant_signature_ip", length = 45)
    private String tenantSignatureIp;

    @Column(name = "agreement_pdf_url", length = 500)
    private String agreementPdfUrl;

    @Column(name = "stamp_duty_paise", nullable = false)
    @Builder.Default
    private long stampDutyPaise = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
