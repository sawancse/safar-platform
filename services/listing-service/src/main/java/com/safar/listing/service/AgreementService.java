package com.safar.listing.service;

import com.safar.listing.dto.*;
import com.safar.listing.entity.AgreementParty;
import com.safar.listing.entity.AgreementRequest;
import com.safar.listing.entity.StampDutyConfig;
import com.safar.listing.entity.enums.*;
import com.safar.listing.repository.AgreementPartyRepository;
import com.safar.listing.repository.AgreementRequestRepository;
import com.safar.listing.repository.StampDutyConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgreementService {

    private final AgreementRequestRepository agreementRequestRepository;
    private final AgreementPartyRepository agreementPartyRepository;
    private final StampDutyConfigRepository stampDutyConfigRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final org.springframework.core.env.Environment env;

    // ── Create ───────��───────────────────────────────────────

    @Transactional
    public AgreementResponse create(CreateAgreementRequest req, UUID userId) {
        AgreementType type = AgreementType.valueOf(req.agreementType());
        AgreementPackage pkg = AgreementPackage.valueOf(req.packageType());

        // Calculate stamp duty from propertyDetailsJson state
        String state = extractStateFromJson(req.propertyDetailsJson());
        StampDutyCalculation dutyCalc = calculateStampDutyInternal(state, type, 0L);

        long serviceFees = getServiceFee(pkg);
        long total = dutyCalc.stampDutyPaise() + dutyCalc.registrationFeePaise() + serviceFees;

        AgreementRequest agreement = AgreementRequest.builder()
                .userId(userId)
                .agreementType(type)
                .agreementPackage(pkg)
                .listingId(req.listingId())
                .state(state)
                .status(AgreementStatus.DRAFT)
                .stampDutyPaise(dutyCalc.stampDutyPaise())
                .registrationFeePaise(dutyCalc.registrationFeePaise())
                .serviceFeePaise(serviceFees)
                .totalFeePaise(total)
                .clausesJson(req.clausesJson())
                .termsJson(req.partyDetailsJson())
                .build();

        agreement = agreementRequestRepository.save(agreement);
        log.info("Agreement created: {} by user {}", agreement.getId(), userId);

        kafkaTemplate.send("agreement.created", agreement.getId().toString(), agreement.getId().toString());

        List<AgreementParty> parties = agreementPartyRepository.findByAgreementRequestId(agreement.getId());
        return toResponse(agreement, parties);
    }

    // ── Get by ID ──────────��─────────────────────────────────

    public AgreementResponse getById(UUID id) {
        AgreementRequest agreement = agreementRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agreement not found: " + id));
        List<AgreementParty> parties = agreementPartyRepository.findByAgreementRequestId(id);
        return toResponse(agreement, parties);
    }

    // ── My Agreements ────────��───────────────────────────────

    public Page<AgreementResponse> getMyAgreements(UUID userId, Pageable pageable) {
        return agreementRequestRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(a -> toResponse(a, agreementPartyRepository.findByAgreementRequestId(a.getId())));
    }

    // ── Update Draft ─────────────────────────────────────────

    @Transactional
    public AgreementResponse updateDraft(UUID id, CreateAgreementRequest req, UUID userId) {
        AgreementRequest agreement = agreementRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agreement not found: " + id));

        if (agreement.getStatus() != AgreementStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT agreements can be updated");
        }
        if (!agreement.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized");
        }

        if (req.agreementType() != null) {
            agreement.setAgreementType(AgreementType.valueOf(req.agreementType()));
        }
        if (req.packageType() != null) {
            agreement.setAgreementPackage(AgreementPackage.valueOf(req.packageType()));
        }
        if (req.clausesJson() != null) {
            agreement.setClausesJson(req.clausesJson());
        }
        if (req.partyDetailsJson() != null) {
            agreement.setTermsJson(req.partyDetailsJson());
        }

        // Recalculate fees
        String state = req.propertyDetailsJson() != null
                ? extractStateFromJson(req.propertyDetailsJson())
                : agreement.getState();
        agreement.setState(state);

        StampDutyCalculation dutyCalc = calculateStampDutyInternal(state, agreement.getAgreementType(), 0L);
        long serviceFees = getServiceFee(agreement.getAgreementPackage());
        agreement.setStampDutyPaise(dutyCalc.stampDutyPaise());
        agreement.setRegistrationFeePaise(dutyCalc.registrationFeePaise());
        agreement.setServiceFeePaise(serviceFees);
        agreement.setTotalFeePaise(dutyCalc.stampDutyPaise() + dutyCalc.registrationFeePaise() + serviceFees);

        agreement = agreementRequestRepository.save(agreement);
        log.info("Agreement draft updated: {}", id);

        List<AgreementParty> parties = agreementPartyRepository.findByAgreementRequestId(id);
        return toResponse(agreement, parties);
    }

    // ── Add Party ──────────────���─────────────────────────────

    @Transactional
    public AgreementPartyResponse addParty(UUID agreementId, AgreementPartyRequest req) {
        agreementRequestRepository.findById(agreementId)
                .orElseThrow(() -> new RuntimeException("Agreement not found: " + agreementId));

        AgreementParty party = AgreementParty.builder()
                .agreementRequestId(agreementId)
                .partyType(PartyType.valueOf(req.partyType()))
                .fullName(req.fullName())
                .aadhaarNumber(req.aadhaarNumber())
                .panNumber(req.panNumber())
                .address(req.address())
                .phone(req.phone())
                .email(req.email())
                .eSignStatus(ESignStatus.PENDING)
                .build();

        party = agreementPartyRepository.save(party);
        log.info("Party added to agreement {}: {}", agreementId, party.getId());

        return toPartyResponse(party);
    }

    // ── Generate Draft ───────────────────────────────────────

    @Transactional
    public AgreementResponse generateDraft(UUID id) {
        AgreementRequest agreement = agreementRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agreement not found: " + id));

        agreement.setStatus(AgreementStatus.DRAFT);
        agreement.setDocumentUrl("/api/v1/agreements/" + id + "/document/draft.pdf");
        agreement = agreementRequestRepository.save(agreement);

        log.info("Draft generated for agreement: {}", id);
        kafkaTemplate.send("agreement.draft.generated", id.toString(), id.toString());

        List<AgreementParty> parties = agreementPartyRepository.findByAgreementRequestId(id);
        return toResponse(agreement, parties);
    }

    // ── Process Payment ──────────────��───────────────────────

    @Transactional
    public AgreementResponse processPayment(UUID id, UUID paymentId) {
        AgreementRequest agreement = agreementRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agreement not found: " + id));

        agreement.setRazorpayPaymentId(paymentId.toString());
        agreement.setStatus(AgreementStatus.STAMPED);
        agreement = agreementRequestRepository.save(agreement);

        log.info("Payment processed for agreement {}: paymentId={}", id, paymentId);
        kafkaTemplate.send("agreement.payment.processed", id.toString(), id);

        List<AgreementParty> parties = agreementPartyRepository.findByAgreementRequestId(id);
        return toResponse(agreement, parties);
    }

    // ── Init E-Sign ───────────���──────────────────────────────

    @Transactional
    public AgreementPartyResponse initESign(UUID id, UUID partyId) {
        AgreementRequest agreement = agreementRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agreement not found: " + id));

        AgreementParty party = agreementPartyRepository.findById(partyId)
                .orElseThrow(() -> new RuntimeException("Party not found: " + partyId));

        party.setESignStatus(ESignStatus.SIGNED);
        party.setSignedAt(OffsetDateTime.now());
        party = agreementPartyRepository.save(party);

        log.info("Party {} signed agreement {}", partyId, id);

        // Check if all parties have signed
        List<AgreementParty> allParties = agreementPartyRepository.findByAgreementRequestId(id);
        boolean allSigned = allParties.stream()
                .allMatch(p -> p.getESignStatus() == ESignStatus.SIGNED);

        if (allSigned) {
            agreement.setStatus(AgreementStatus.SIGNED);
            agreementRequestRepository.save(agreement);
            log.info("All parties signed. Agreement {} is now SIGNED", id);
            kafkaTemplate.send("agreement.signed", id.toString(), id);
        }

        return toPartyResponse(party);
    }

    // ── Calculate Stamp Duty ─────────────────────────────────

    public StampDutyCalculation calculateStampDuty(String state, String agreementType, Long propertyValuePaise) {
        AgreementType type = AgreementType.valueOf(agreementType);
        return calculateStampDutyInternal(state, type, propertyValuePaise != null ? propertyValuePaise : 0L);
    }

    // ── Admin: List All ────────────────────────────────────────

    public Page<Map<String, Object>> adminListAll(String status, String type, Pageable pageable) {
        Page<AgreementRequest> page;

        if (status != null && !status.isBlank() && type != null && !type.isBlank()) {
            page = agreementRequestRepository.findByStatusAndAgreementTypeOrderByCreatedAtDesc(
                    AgreementStatus.valueOf(status), AgreementType.valueOf(type), pageable);
        } else if (status != null && !status.isBlank()) {
            page = agreementRequestRepository.findByStatusOrderByCreatedAtDesc(
                    AgreementStatus.valueOf(status), pageable);
        } else if (type != null && !type.isBlank()) {
            page = agreementRequestRepository.findByAgreementTypeOrderByCreatedAtDesc(
                    AgreementType.valueOf(type), pageable);
        } else {
            page = agreementRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        // Batch-fetch user contacts
        Set<UUID> userIds = new HashSet<>();
        page.forEach(a -> userIds.add(a.getUserId()));
        Map<UUID, Map<String, String>> contacts = fetchUserContacts(userIds);

        return page.map(a -> {
            AgreementResponse resp = toResponse(a, agreementPartyRepository.findByAgreementRequestId(a.getId()));
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", resp.id());
            map.put("userId", resp.userId());
            map.put("agreementType", resp.agreementType());
            map.put("propertyId", resp.propertyId());
            map.put("listingId", resp.listingId());
            map.put("packageType", resp.packageType());
            map.put("partyDetailsJson", resp.partyDetailsJson());
            map.put("propertyDetailsJson", resp.propertyDetailsJson());
            map.put("clausesJson", resp.clausesJson());
            map.put("status", resp.status());
            map.put("draftPdfUrl", resp.draftPdfUrl());
            map.put("signedPdfUrl", resp.signedPdfUrl());
            map.put("registeredPdfUrl", resp.registeredPdfUrl());
            map.put("stampDutyPaise", resp.stampDutyPaise());
            map.put("registrationFeePaise", resp.registrationFeePaise());
            map.put("serviceFeePaise", resp.serviceFeePaise());
            map.put("totalAmountPaise", resp.totalAmountPaise());
            map.put("rejectionReason", resp.rejectionReason());
            map.put("signedAt", resp.signedAt());
            map.put("registeredAt", resp.registeredAt());
            map.put("createdAt", resp.createdAt());
            map.put("updatedAt", resp.updatedAt());
            map.put("parties", resp.parties());
            // Entity-level fields not in DTO
            map.put("state", a.getState());
            map.put("city", a.getCity());
            map.put("agreementDate", a.getAgreementDate());
            map.put("startDate", a.getStartDate());
            map.put("endDate", a.getEndDate());
            map.put("monthlyRentPaise", a.getMonthlyRentPaise());
            map.put("securityDepositPaise", a.getSecurityDepositPaise());
            map.put("saleConsiderationPaise", a.getSaleConsiderationPaise());
            map.put("stampCertificateNumber", a.getStampCertificateNumber());
            map.put("razorpayPaymentId", a.getRazorpayPaymentId());
            map.put("notes", a.getNotes());
            // Enriched user contact
            Map<String, String> contact = contacts.getOrDefault(a.getUserId(), Map.of());
            map.put("userName", contact.getOrDefault("name", ""));
            map.put("userPhone", contact.getOrDefault("phone", ""));
            map.put("userEmail", contact.getOrDefault("email", ""));
            return map;
        });
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, Map<String, String>> fetchUserContacts(Set<UUID> userIds) {
        Map<UUID, Map<String, String>> result = new HashMap<>();
        String userUrl = env.getProperty("services.user-service.url");
        if (userUrl == null || userUrl.isBlank() || userIds.isEmpty()) return result;
        org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
        for (UUID uid : userIds) {
            try {
                Map<String, String> contact = rt.getForObject(
                        userUrl + "/api/v1/internal/users/" + uid + "/contact", Map.class);
                if (contact != null) result.put(uid, contact);
            } catch (Exception e) {
                log.warn("Failed to fetch contact for user {}: {}", uid, e.getMessage());
            }
        }
        return result;
    }

    // ── Admin: Update Status ─────────────────────────────────

    @Transactional
    public AgreementResponse adminUpdateStatus(UUID id, String newStatus) {
        AgreementRequest agreement = agreementRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agreement not found: " + id));

        AgreementStatus target = AgreementStatus.valueOf(newStatus);
        agreement.setStatus(target);
        agreement = agreementRequestRepository.save(agreement);

        log.info("Admin updated agreement {} status to {}", id, newStatus);
        kafkaTemplate.send("agreement.status.updated", id.toString(), newStatus);

        List<AgreementParty> parties = agreementPartyRepository.findByAgreementRequestId(id);
        return toResponse(agreement, parties);
    }

    // ── Private Helpers ──────────────────────────────────────

    private StampDutyCalculation calculateStampDutyInternal(String state, AgreementType type, Long propertyValuePaise) {
        var configOpt = stampDutyConfigRepository.findByStateAndAgreementTypeAndActiveTrue(state, type);

        if (configOpt.isEmpty()) {
            // Default: 5% stamp duty, 1% registration
            long stampDuty = propertyValuePaise * 5 / 100;
            long regFee = propertyValuePaise / 100;
            return new StampDutyCalculation(state, type.name(), propertyValuePaise,
                    stampDuty, regFee, 0L, stampDuty + regFee);
        }

        StampDutyConfig config = configOpt.get();
        long stampDuty = config.getStampDutyPercent()
                .multiply(BigDecimal.valueOf(propertyValuePaise))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING)
                .longValue();

        if (config.getMinimumStampPaise() != null && stampDuty < config.getMinimumStampPaise()) {
            stampDuty = config.getMinimumStampPaise();
        }
        if (config.getMaximumStampPaise() != null && stampDuty > config.getMaximumStampPaise()) {
            stampDuty = config.getMaximumStampPaise();
        }

        long regFee = config.getRegistrationPercent()
                .multiply(BigDecimal.valueOf(propertyValuePaise))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING)
                .longValue();

        long surcharge = 0L;
        if (config.getSurchargePercent() != null) {
            surcharge = config.getSurchargePercent()
                    .multiply(BigDecimal.valueOf(stampDuty))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING)
                    .longValue();
        }

        return new StampDutyCalculation(state, type.name(), propertyValuePaise,
                stampDuty, regFee, surcharge, stampDuty + regFee + surcharge);
    }

    private long getServiceFee(AgreementPackage pkg) {
        return switch (pkg) {
            case BASIC -> 0L;
            case ESTAMP -> 149900L;
            case REGISTERED -> 499900L;
            case PREMIUM -> 999900L;
        };
    }

    private String extractStateFromJson(String propertyDetailsJson) {
        if (propertyDetailsJson == null || propertyDetailsJson.isBlank()) {
            return "Maharashtra";
        }
        // Simple extraction — look for "state":"value"
        try {
            int idx = propertyDetailsJson.indexOf("\"state\"");
            if (idx >= 0) {
                int colonIdx = propertyDetailsJson.indexOf(":", idx);
                int firstQuote = propertyDetailsJson.indexOf("\"", colonIdx + 1);
                int secondQuote = propertyDetailsJson.indexOf("\"", firstQuote + 1);
                if (firstQuote >= 0 && secondQuote > firstQuote) {
                    return propertyDetailsJson.substring(firstQuote + 1, secondQuote);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract state from propertyDetailsJson, defaulting to Maharashtra");
        }
        return "Maharashtra";
    }

    private AgreementResponse toResponse(AgreementRequest a, List<AgreementParty> parties) {
        List<AgreementPartyResponse> partyResponses = parties.stream()
                .map(this::toPartyResponse)
                .toList();

        return new AgreementResponse(
                a.getId(),
                a.getUserId(),
                a.getAgreementType(),
                a.getSalePropertyId(),
                a.getListingId(),
                a.getAgreementPackage() != null ? a.getAgreementPackage().name() : null,
                a.getTermsJson(),
                null, // propertyDetailsJson not stored separately
                a.getClausesJson(),
                a.getStatus(),
                a.getDocumentUrl(),
                a.getSignedDocumentUrl(),
                a.getRegisteredDocumentUrl(),
                a.getStampDutyPaise(),
                a.getRegistrationFeePaise(),
                a.getServiceFeePaise(),
                a.getTotalFeePaise(),
                null, // rejectionReason
                a.getStatus() == AgreementStatus.SIGNED ? a.getUpdatedAt() : null,
                a.getStatus() == AgreementStatus.REGISTERED ? a.getUpdatedAt() : null,
                a.getCreatedAt(),
                a.getUpdatedAt(),
                partyResponses
        );
    }

    private AgreementPartyResponse toPartyResponse(AgreementParty p) {
        return new AgreementPartyResponse(
                p.getId(),
                p.getAgreementRequestId(),
                p.getPartyType() != null ? p.getPartyType().name() : null,
                p.getFullName(),
                p.getAadhaarNumber(),
                p.getPanNumber(),
                p.getAddress(),
                p.getPhone(),
                p.getEmail(),
                p.getESignStatus() == ESignStatus.SIGNED,
                p.getCreatedAt()
        );
    }
}
