package com.safar.booking.service;

import com.safar.booking.dto.AgreementResponse;
import com.safar.booking.dto.CreateAgreementRequest;
import com.safar.booking.entity.PgTenancy;
import com.safar.booking.entity.TenancyAgreement;
import com.safar.booking.entity.enums.AgreementStatus;
import com.safar.booking.kafka.KafkaJsonPublisher;
import com.safar.booking.repository.TenancyAgreementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenancyAgreementService {

    private final TenancyAgreementRepository agreementRepository;
    private final PgTenancyService tenancyService;
    private final KafkaJsonPublisher kafkaJsonPublisher;

    private static long agreementCounter = 1000;

    @Transactional
    public TenancyAgreement createAgreement(UUID tenancyId, CreateAgreementRequest req) {
        PgTenancy tenancy = tenancyService.getTenancy(tenancyId);

        if (agreementRepository.findByTenancyId(tenancyId).isPresent()) {
            throw new RuntimeException("Agreement already exists for tenancy: " + tenancyId);
        }

        String agreementText = generateAgreementText(tenancy, req);

        TenancyAgreement agreement = TenancyAgreement.builder()
                .tenancyId(tenancyId)
                .agreementNumber("AGR-" + LocalDate.now().getYear() + "-" + String.format("%04d", ++agreementCounter))
                .tenantName(req.tenantName())
                .tenantPhone(req.tenantPhone())
                .tenantEmail(req.tenantEmail())
                .tenantAadhaarLast4(req.tenantAadhaarLast4())
                .hostName(req.hostName())
                .hostPhone(req.hostPhone())
                .propertyAddress(req.propertyAddress())
                .roomDescription(req.roomDescription())
                .moveInDate(tenancy.getMoveInDate())
                .lockInPeriodMonths(req.lockInPeriodMonths() != null ? req.lockInPeriodMonths() : 0)
                .noticePeriodDays(tenancy.getNoticePeriodDays())
                .monthlyRentPaise(tenancy.getMonthlyRentPaise())
                .securityDepositPaise(tenancy.getSecurityDepositPaise())
                .maintenanceChargesPaise(req.maintenanceChargesPaise() != null ? req.maintenanceChargesPaise() : 0)
                .agreementText(agreementText)
                .termsAndConditions(req.termsAndConditions())
                .status(AgreementStatus.PENDING_HOST_SIGN)
                .build();

        TenancyAgreement saved = agreementRepository.save(agreement);

        // Enrich Kafka event with tenantId
        java.util.Map<String, Object> event = buildAgreementEvent(saved, tenancy);
        kafkaJsonPublisher.publish("tenancy.agreement.created", saved.getId().toString(), event);
        log.info("Agreement {} created for tenancy {}", saved.getAgreementNumber(), tenancy.getTenancyRef());
        return saved;
    }

    @Transactional
    public TenancyAgreement hostSign(UUID tenancyId, UUID userId, String signatureIp) {
        TenancyAgreement agreement = getByTenancyId(tenancyId);
        PgTenancy tenancy = tenancyService.getTenancy(tenancyId);

        // Verify the signer is the host — tenant cannot sign as host
        // Host owns the listing, tenant is on the tenancy; only non-tenant can sign as host
        if (tenancy.getTenantId().equals(userId)) {
            throw new RuntimeException("Tenant cannot sign as host. Use tenant-sign endpoint.");
        }

        if (agreement.getStatus() != AgreementStatus.PENDING_HOST_SIGN) {
            throw new RuntimeException("Agreement not in signable state for host. Current: " + agreement.getStatus());
        }

        agreement.setHostSignedAt(OffsetDateTime.now());
        agreement.setHostSignatureIp(signatureIp);
        agreement.setHostSignedBy(userId);
        agreement.setStatus(AgreementStatus.PENDING_TENANT_SIGN);

        TenancyAgreement saved = agreementRepository.save(agreement);

        // Enrich Kafka event with tenantId for notification-service
        java.util.Map<String, Object> event = buildAgreementEvent(saved, tenancy);
        kafkaJsonPublisher.publish("tenancy.agreement.host-signed", saved.getId().toString(), event);
        log.info("Host (userId={}) signed agreement {}", userId, saved.getAgreementNumber());
        return saved;
    }

    @Transactional
    public TenancyAgreement tenantSign(UUID tenancyId, UUID userId, String signatureIp) {
        TenancyAgreement agreement = getByTenancyId(tenancyId);
        PgTenancy tenancy = tenancyService.getTenancy(tenancyId);

        // Verify the signer is the actual tenant for this tenancy
        if (!tenancy.getTenantId().equals(userId)) {
            throw new RuntimeException("Only the tenant of this tenancy can sign. userId=" + userId);
        }

        if (agreement.getStatus() != AgreementStatus.PENDING_TENANT_SIGN) {
            throw new RuntimeException("Host must sign first. Current status: " + agreement.getStatus());
        }

        agreement.setTenantSignedAt(OffsetDateTime.now());
        agreement.setTenantSignatureIp(signatureIp);
        agreement.setTenantSignedBy(userId);
        agreement.setStatus(AgreementStatus.ACTIVE);

        TenancyAgreement saved = agreementRepository.save(agreement);

        // Enrich Kafka event with tenantId for notification-service
        java.util.Map<String, Object> event = buildAgreementEvent(saved, tenancy);
        kafkaJsonPublisher.publish("tenancy.agreement.active", saved.getId().toString(), event);
        log.info("Tenant (userId={}) signed agreement {} — now ACTIVE", userId, saved.getAgreementNumber());
        return saved;
    }

    private java.util.Map<String, Object> buildAgreementEvent(TenancyAgreement a, PgTenancy tenancy) {
        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put("id", a.getId().toString());
        event.put("tenancyId", a.getTenancyId().toString());
        event.put("tenantId", tenancy.getTenantId().toString());
        event.put("agreementNumber", a.getAgreementNumber());
        event.put("status", a.getStatus().name());
        event.put("tenantName", a.getTenantName());
        event.put("tenantEmail", a.getTenantEmail() != null ? a.getTenantEmail() : "");
        event.put("hostName", a.getHostName());
        event.put("propertyAddress", a.getPropertyAddress());
        event.put("roomDescription", a.getRoomDescription() != null ? a.getRoomDescription() : "");
        event.put("monthlyRentPaise", a.getMonthlyRentPaise());
        event.put("moveInDate", a.getMoveInDate() != null ? a.getMoveInDate().toString() : "");
        if (a.getHostSignedAt() != null) event.put("hostSignedAt", a.getHostSignedAt().toString());
        if (a.getTenantSignedAt() != null) event.put("tenantSignedAt", a.getTenantSignedAt().toString());
        return event;
    }

    public TenancyAgreement getByTenancyId(UUID tenancyId) {
        return agreementRepository.findByTenancyId(tenancyId)
                .orElseThrow(() -> new RuntimeException("No agreement found for tenancy: " + tenancyId));
    }

    public AgreementResponse toResponse(TenancyAgreement a) {
        return new AgreementResponse(
                a.getId(), a.getTenancyId(), a.getAgreementNumber(),
                a.getStatus().name(),
                a.getTenantName(), a.getTenantPhone(), a.getTenantEmail(),
                a.getHostName(), a.getHostPhone(),
                a.getPropertyAddress(), a.getRoomDescription(),
                a.getMoveInDate(), a.getLockInPeriodMonths(), a.getNoticePeriodDays(),
                a.getMonthlyRentPaise(), a.getSecurityDepositPaise(),
                a.getMaintenanceChargesPaise(), a.getStampDutyPaise(),
                a.getHostSignedAt(), a.getTenantSignedAt(),
                a.getAgreementPdfUrl(), a.getCreatedAt()
        );
    }

    private String generateAgreementText(PgTenancy tenancy, CreateAgreementRequest req) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        String moveInFormatted = tenancy.getMoveInDate().format(fmt);
        long rentInr = tenancy.getMonthlyRentPaise() / 100;
        long depositInr = tenancy.getSecurityDepositPaise() / 100;
        long maintenanceInr = (req.maintenanceChargesPaise() != null ? req.maintenanceChargesPaise() : 0) / 100;
        int lockIn = req.lockInPeriodMonths() != null ? req.lockInPeriodMonths() : 0;

        return """
                RENTAL / PAYING GUEST AGREEMENT
                ================================

                This Rental Agreement ("Agreement") is entered into on %s

                BETWEEN:

                OWNER / HOST (hereinafter referred to as "Licensor"):
                Name: %s
                Contact: %s
                Property Address: %s

                AND

                TENANT / PAYING GUEST (hereinafter referred to as "Licensee"):
                Name: %s
                Contact: %s
                Email: %s
                Aadhaar (last 4 digits): %s

                WHEREAS the Licensor is the owner/authorized representative of the above-mentioned property and agrees to grant a license to the Licensee for residential use on the following terms and conditions:

                1. PREMISES
                   The Licensor hereby grants the Licensee a license to use and occupy:
                   Room/Bed: %s
                   Sharing Type: %s
                   at the property mentioned above.

                2. COMMENCEMENT & DURATION
                   - Move-in Date: %s
                   - Lock-in Period: %d month(s)
                   - Notice Period: %d days
                   The agreement shall be valid from the move-in date until terminated by either party with the required notice period.

                3. MONTHLY RENT
                   - Base Rent: INR %,d per month
                   - Maintenance Charges: INR %,d per month
                   - Total Monthly Payable: INR %,d per month
                   Rent is due on the %s of every month. Payment must be made between the 1st and 5th of each month. A grace period of %d day(s) from the due date is allowed, after which a late fee of %.2f%%%% per day will apply (capped at %d%%%% of the invoice total).

                4. SECURITY DEPOSIT
                   - Amount: INR %,d
                   The security deposit shall be collected at the time of move-in and will be refunded within 30 days of vacating, after deducting any dues, damages, or unpaid rent.

                5. INCLUSIONS
                   - Meals: %s
                   - Laundry: %s
                   - WiFi: %s

                6. HOUSE RULES
                   a) Gate closing time must be adhered to as set by the property.
                   b) No subletting or transfer of the license to any third party.
                   c) The Licensee shall maintain the premises in good condition.
                   d) Any damage caused by the Licensee shall be repaired at their expense.
                   e) The Licensor reserves the right to inspect the premises with prior notice.
                   f) Illegal activities, substance abuse, and loud noise after 10 PM are strictly prohibited.
                   g) Guests are allowed only in common areas and during permitted hours.

                7. TERMINATION
                   a) Either party may terminate this agreement by providing %d days' written notice.
                   b) During the lock-in period of %d months, early termination by the Licensee will result in forfeiture of %d month(s)' rent from the security deposit.
                   c) The Licensor may terminate immediately for violation of house rules or non-payment exceeding 15 days.

                8. SETTLEMENT ON VACATING
                   Upon vacating, a final settlement will be prepared including:
                   - Outstanding rent and utility charges
                   - Damage assessment (if any)
                   - Late payment penalties (if any)
                   - Refund of security deposit after deductions

                9. JURISDICTION
                   This agreement shall be governed by the laws of India. Any disputes shall be subject to the jurisdiction of the courts in the city where the property is located.

                10. DIGITAL SIGNATURES
                    Both parties agree that digital signatures on this platform constitute valid and binding consent to the terms of this agreement, in accordance with the Information Technology Act, 2000.

                %s

                ---
                Licensor Signature: [Pending]
                Licensee Signature: [Pending]

                Generated via Safar Platform | Agreement Reference: [Auto-assigned]
                """.formatted(
                moveInFormatted,
                req.hostName(), req.hostPhone() != null ? req.hostPhone() : "N/A",
                req.propertyAddress(),
                req.tenantName(), req.tenantPhone() != null ? req.tenantPhone() : "N/A",
                req.tenantEmail() != null ? req.tenantEmail() : "N/A",
                req.tenantAadhaarLast4() != null ? "XXXX-XXXX-" + req.tenantAadhaarLast4() : "Not provided",
                req.roomDescription() != null ? req.roomDescription() : "As assigned",
                tenancy.getSharingType(),
                moveInFormatted,
                lockIn, tenancy.getNoticePeriodDays(),
                rentInr, maintenanceInr, rentInr + maintenanceInr,
                ordinalSuffix(tenancy.getBillingDay()),
                tenancy.getGracePeriodDays(),
                tenancy.getLatePenaltyBps() / 100.0,
                tenancy.getMaxPenaltyPercent(),
                depositInr,
                tenancy.isMealsIncluded() ? "Included" : "Not Included",
                tenancy.isLaundryIncluded() ? "Included" : "Not Included",
                tenancy.isWifiIncluded() ? "Included" : "Not Included",
                tenancy.getNoticePeriodDays(), lockIn, Math.max(1, lockIn / 2),
                req.termsAndConditions() != null ? "\nADDITIONAL TERMS:\n" + req.termsAndConditions() : ""
        );
    }

    private String ordinalSuffix(int day) {
        if (day >= 11 && day <= 13) return day + "th";
        return switch (day % 10) {
            case 1 -> day + "st";
            case 2 -> day + "nd";
            case 3 -> day + "rd";
            default -> day + "th";
        };
    }
}
