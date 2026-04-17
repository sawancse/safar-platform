package com.safar.listing.service;

import com.safar.listing.dto.*;
import com.safar.listing.entity.*;
import com.safar.listing.entity.enums.*;
import com.safar.listing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalService {

    private final LegalCaseRepository legalCaseRepository;
    private final LegalDocRepository legalDocRepository;
    private final LegalVerificationRepository legalVerificationRepository;
    private final AdvocateRepository advocateRepository;
    private final LegalConsultationRepository legalConsultationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ── Create Case ──────────────────────────────────────────

    @Transactional
    public LegalCaseResponse createCase(CreateLegalCaseRequest req, UUID userId) {
        LegalPackageType pkg = LegalPackageType.valueOf(req.packageType());
        long serviceFees = getServiceFee(pkg);

        LegalCase legalCase = LegalCase.builder()
                .userId(userId)
                .packageType(pkg)
                .salePropertyId(req.propertyId())
                .propertyAddress(req.propertyAddress())
                .city(req.propertyCity())
                .state(req.propertyState())
                .status(LegalCaseStatus.CREATED)
                .feePaise(serviceFees)
                .riskLevel(RiskLevel.GREEN)
                .build();

        legalCase = legalCaseRepository.save(legalCase);

        // Create default verifications based on package
        createDefaultVerifications(legalCase.getId(), pkg);

        log.info("Legal case created: {} by user {} with package {}", legalCase.getId(), userId, pkg);
        kafkaTemplate.send("legal.case.created", legalCase.getId().toString(), legalCase.getId().toString());

        return toCaseResponse(legalCase);
    }

    // ── All Cases (Admin) ─────────────────────────────────────

    public Page<LegalCaseResponse> getAllCases(String status, String packageType, String riskLevel, Pageable pageable) {
        if (status != null && !status.isBlank() && packageType != null && !packageType.isBlank()) {
            return legalCaseRepository.findByStatusAndPackageTypeOrderByCreatedAtDesc(
                    LegalCaseStatus.valueOf(status), LegalPackageType.valueOf(packageType), pageable)
                    .map(this::toCaseResponse);
        }
        if (status != null && !status.isBlank()) {
            return legalCaseRepository.findByStatusOrderByCreatedAtDesc(
                    LegalCaseStatus.valueOf(status), pageable)
                    .map(this::toCaseResponse);
        }
        if (packageType != null && !packageType.isBlank()) {
            return legalCaseRepository.findByPackageTypeOrderByCreatedAtDesc(
                    LegalPackageType.valueOf(packageType), pageable)
                    .map(this::toCaseResponse);
        }
        if (riskLevel != null && !riskLevel.isBlank()) {
            return legalCaseRepository.findByRiskLevelOrderByCreatedAtDesc(
                    RiskLevel.valueOf(riskLevel), pageable)
                    .map(this::toCaseResponse);
        }
        return legalCaseRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toCaseResponse);
    }

    // ── Get Case ─────────────────────────────────────────────

    public LegalCaseResponse getCase(UUID id) {
        LegalCase legalCase = legalCaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Legal case not found: " + id));
        return toCaseResponse(legalCase);
    }

    // ── My Cases ─────────────────────────────────────────────

    public Page<LegalCaseResponse> getMyCases(UUID userId, Pageable pageable) {
        return legalCaseRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toCaseResponse);
    }

    // ── Upload Document ──────────────────────────────────────

    @Transactional
    public void uploadDocument(UUID caseId, LegalDocumentRequest req, UUID userId) {
        LegalCase legalCase = legalCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Legal case not found: " + caseId));

        LegalDocument doc = LegalDocument.builder()
                .legalCaseId(caseId)
                .docType(LegalDocType.valueOf(req.documentType()))
                .fileName(req.documentType() + "_" + System.currentTimeMillis())
                .fileUrl(req.fileUrl())
                .uploadedByUser(true)
                .build();

        legalDocRepository.save(doc);

        if (legalCase.getStatus() == LegalCaseStatus.CREATED) {
            legalCase.setStatus(LegalCaseStatus.DOCUMENTS_UPLOADED);
            legalCaseRepository.save(legalCase);
        }

        log.info("Document uploaded for case {}: type={}", caseId, req.documentType());
    }

    // ── Get Case Documents ───────────────────────────────────

    public List<LegalDocument> getCaseDocuments(UUID caseId) {
        return legalDocRepository.findByLegalCaseId(caseId);
    }

    // ── Assign Advocate (Admin) ──────────────────────────────

    @Transactional
    public LegalCaseResponse assignAdvocate(UUID caseId, UUID advocateId) {
        LegalCase legalCase = legalCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Legal case not found: " + caseId));

        advocateRepository.findById(advocateId)
                .orElseThrow(() -> new RuntimeException("Advocate not found: " + advocateId));

        legalCase.setAdvocateId(advocateId);
        legalCase.setStatus(LegalCaseStatus.ASSIGNED);
        legalCase.setAssignedAt(OffsetDateTime.now());
        legalCase = legalCaseRepository.save(legalCase);

        log.info("Advocate {} assigned to case {}", advocateId, caseId);
        kafkaTemplate.send("legal.advocate.assigned", caseId.toString(), advocateId.toString());

        return toCaseResponse(legalCase);
    }

    // ── Update Case Status (Admin) ───────────────────────────

    @Transactional
    public LegalCaseResponse updateCaseStatus(UUID caseId, String status) {
        LegalCase legalCase = legalCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Legal case not found: " + caseId));

        LegalCaseStatus newStatus = LegalCaseStatus.valueOf(status);
        legalCase.setStatus(newStatus);

        if (newStatus == LegalCaseStatus.CLOSED) {
            legalCase.setClosedAt(OffsetDateTime.now());
        }

        legalCase = legalCaseRepository.save(legalCase);
        log.info("Case {} status updated to {}", caseId, status);

        return toCaseResponse(legalCase);
    }

    // ── Update Verification (Admin) ──────────────────────────

    @Transactional
    public LegalVerificationResponse updateVerification(UUID verificationId, String status, String findingsJson) {
        LegalVerification verification = legalVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new RuntimeException("Verification not found: " + verificationId));

        verification.setStatus(VerificationStatus.valueOf(status));
        verification.setFindings(findingsJson);
        verification.setVerifiedAt(OffsetDateTime.now());

        if (VerificationStatus.valueOf(status) == VerificationStatus.ISSUE_FOUND) {
            verification.setRiskLevel(RiskLevel.RED);
        } else if (VerificationStatus.valueOf(status) == VerificationStatus.CLEAN) {
            verification.setRiskLevel(RiskLevel.GREEN);
        }

        verification = legalVerificationRepository.save(verification);
        log.info("Verification {} updated: status={}", verificationId, status);

        return toVerificationResponse(verification);
    }

    // ── Generate Report (Admin) ──────────────────────────────

    @Transactional
    public LegalCaseResponse generateReport(UUID caseId) {
        LegalCase legalCase = legalCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Legal case not found: " + caseId));

        List<LegalVerification> verifications = legalVerificationRepository.findByLegalCaseId(caseId);

        // Analyze verifications for risk level — count severity, not just presence
        long issueCount = verifications.stream()
                .filter(v -> v.getStatus() == VerificationStatus.ISSUE_FOUND).count();
        boolean allClean = verifications.stream()
                .allMatch(v -> v.getStatus() == VerificationStatus.CLEAN);
        long pendingCount = verifications.stream()
                .filter(v -> v.getStatus() == VerificationStatus.PENDING).count();

        if (allClean) {
            legalCase.setRiskLevel(RiskLevel.GREEN);
        } else if (issueCount >= 2) {
            legalCase.setRiskLevel(RiskLevel.RED);     // Multiple issues = high risk
        } else if (issueCount == 1) {
            legalCase.setRiskLevel(RiskLevel.YELLOW);  // Single issue = medium risk
        } else if (pendingCount > 0) {
            legalCase.setRiskLevel(RiskLevel.YELLOW);  // Still pending = yellow
        } else {
            legalCase.setRiskLevel(RiskLevel.GREEN);
        }

        legalCase.setStatus(LegalCaseStatus.REPORT_READY);
        legalCase.setReportReadyAt(OffsetDateTime.now());
        legalCase.setReportUrl("/api/v1/legal/cases/" + caseId + "/report.pdf");

        StringBuilder summary = new StringBuilder();
        summary.append("Legal verification report for property at ")
                .append(legalCase.getPropertyAddress()).append("\n");
        summary.append("Risk Level: ").append(legalCase.getRiskLevel()).append("\n");
        summary.append("Verifications: ").append(verifications.size()).append("\n");
        for (LegalVerification v : verifications) {
            summary.append("- ").append(v.getVerificationType())
                    .append(": ").append(v.getStatus()).append("\n");
        }
        legalCase.setReportSummary(summary.toString());

        legalCase = legalCaseRepository.save(legalCase);
        log.info("Report generated for case {}: riskLevel={}", caseId, legalCase.getRiskLevel());

        kafkaTemplate.send("legal.report.ready", caseId.toString(), caseId.toString());

        return toCaseResponse(legalCase);
    }

    // ── Generate Report PDF ────────────────────────────────

    @Transactional
    public byte[] generateReportPdf(UUID caseId, LegalReportPdfService pdfService) {
        LegalCase legalCase = legalCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Legal case not found: " + caseId));

        List<LegalVerification> verifications = legalVerificationRepository.findByLegalCaseId(caseId);

        // Auto-generate report data if not done yet
        if (legalCase.getReportSummary() == null || legalCase.getRiskLevel() == null) {
            boolean anyIssue = verifications.stream()
                    .anyMatch(v -> v.getStatus() == VerificationStatus.ISSUE_FOUND);
            boolean allClean = verifications.stream()
                    .allMatch(v -> v.getStatus() == VerificationStatus.CLEAN);

            if (legalCase.getRiskLevel() == null) {
                if (allClean) legalCase.setRiskLevel(RiskLevel.GREEN);
                else if (anyIssue) legalCase.setRiskLevel(RiskLevel.RED);
                else legalCase.setRiskLevel(RiskLevel.YELLOW);
            }

            if (legalCase.getReportSummary() == null) {
                StringBuilder summary = new StringBuilder();
                summary.append("Legal verification report for property at ")
                        .append(legalCase.getPropertyAddress() != null ? legalCase.getPropertyAddress() : "N/A").append("\n");
                summary.append("Risk Level: ").append(legalCase.getRiskLevel()).append("\n");
                summary.append("Verifications: ").append(verifications.size()).append("\n");
                for (LegalVerification v : verifications) {
                    summary.append("- ").append(v.getVerificationType())
                            .append(": ").append(v.getStatus()).append("\n");
                }
                legalCase.setReportSummary(summary.toString());
            }

            if (legalCase.getReportReadyAt() == null) {
                legalCase.setReportReadyAt(OffsetDateTime.now());
            }
            legalCaseRepository.save(legalCase);
        }

        return pdfService.generatePdf(legalCase, verifications);
    }

    // ── Schedule Consultation ────────────────────────────────

    @Transactional
    public ConsultationResponse scheduleConsultation(UUID caseId, ScheduleConsultationRequest req, UUID userId) {
        LegalCase legalCase = legalCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Legal case not found: " + caseId));

        UUID advocateId = legalCase.getAdvocateId();
        if (advocateId == null) {
            throw new IllegalStateException("An advocate must be assigned before scheduling a consultation. Please wait for advocate assignment.");
        }

        Advocate advocate = advocateRepository.findById(advocateId)
                .orElseThrow(() -> new RuntimeException("Advocate not found"));

        LegalConsultation consultation = LegalConsultation.builder()
                .legalCaseId(caseId)
                .advocateId(advocateId)
                .userId(userId)
                .status(ConsultationStatus.SCHEDULED)
                .scheduledAt(req.scheduledAt().atOffset(java.time.ZoneOffset.UTC))
                .durationMinutes(req.durationMinutes() != null ? req.durationMinutes() : 30)
                .feePaise(advocate.getConsultationFeePaise())
                .build();

        consultation = legalConsultationRepository.save(consultation);
        log.info("Consultation scheduled for case {}: {}", caseId, consultation.getId());

        return toConsultationResponse(consultation, advocate);
    }

    // ── List Advocates ───────────────────────────────────────

    public List<AdvocateResponse> listAdvocates(String city) {
        List<Advocate> advocates = (city != null && !city.isBlank())
                ? advocateRepository.findByCityAndActiveTrue(city)
                : advocateRepository.findByActiveTrue();

        return advocates.stream()
                .map(this::toAdvocateResponse)
                .toList();
    }

    // ── List Packages ────────────────────────────────────────

    public List<Map<String, Object>> listPackages() {
        List<Map<String, Object>> packages = new ArrayList<>();

        packages.add(Map.of(
                "type", "TITLE_SEARCH",
                "name", "Title Search",
                "pricePaise", 299900L,
                "description", "Basic title chain verification",
                "features", List.of("Title chain verification", "Encumbrance check")
        ));

        packages.add(Map.of(
                "type", "DUE_DILIGENCE",
                "name", "Due Diligence",
                "pricePaise", 999900L,
                "description", "Comprehensive property due diligence",
                "features", List.of("Title chain verification", "Encumbrance check",
                        "Government approvals", "Litigation check", "Tax verification")
        ));

        packages.add(Map.of(
                "type", "BUYER_ASSIST",
                "name", "Buyer Assist",
                "pricePaise", 1999900L,
                "description", "Full buyer assistance with legal support",
                "features", List.of("All Due Diligence features", "Agreement drafting",
                        "Registration assistance", "1 consultation session")
        ));

        packages.add(Map.of(
                "type", "PREMIUM",
                "name", "Premium",
                "pricePaise", 4999900L,
                "description", "End-to-end legal support",
                "features", List.of("All Buyer Assist features", "Dedicated advocate",
                        "Unlimited consultations", "Post-registration support", "Survey verification")
        ));

        return packages;
    }

    // ── Private Helpers ──────────────────────────────────────

    private long getServiceFee(LegalPackageType pkg) {
        return switch (pkg) {
            case TITLE_SEARCH -> 299900L;
            case DUE_DILIGENCE -> 999900L;
            case BUYER_ASSIST -> 1999900L;
            case PREMIUM -> 4999900L;
        };
    }

    private void createDefaultVerifications(UUID caseId, LegalPackageType pkg) {
        List<VerificationType> types = new ArrayList<>();

        switch (pkg) {
            case TITLE_SEARCH:
                types.add(VerificationType.TITLE_CHAIN);
                types.add(VerificationType.ENCUMBRANCE);
                break;
            case DUE_DILIGENCE:
                types.addAll(List.of(VerificationType.TITLE_CHAIN, VerificationType.ENCUMBRANCE,
                        VerificationType.GOVT_APPROVAL, VerificationType.LITIGATION,
                        VerificationType.TAX));
                break;
            case BUYER_ASSIST:
                types.addAll(List.of(VerificationType.TITLE_CHAIN, VerificationType.ENCUMBRANCE,
                        VerificationType.GOVT_APPROVAL, VerificationType.LITIGATION,
                        VerificationType.TAX));
                break;
            case PREMIUM:
                types.addAll(List.of(VerificationType.TITLE_CHAIN, VerificationType.ENCUMBRANCE,
                        VerificationType.GOVT_APPROVAL, VerificationType.LITIGATION,
                        VerificationType.TAX, VerificationType.SURVEY));
                break;
        }

        for (VerificationType type : types) {
            LegalVerification verification = LegalVerification.builder()
                    .legalCaseId(caseId)
                    .verificationType(type)
                    .status(VerificationStatus.PENDING)
                    .riskLevel(RiskLevel.GREEN)
                    .build();
            legalVerificationRepository.save(verification);
        }
    }

    private LegalCaseResponse toCaseResponse(LegalCase c) {
        List<LegalVerification> verifications = legalVerificationRepository.findByLegalCaseId(c.getId());
        List<LegalDocument> docs = legalDocRepository.findByLegalCaseId(c.getId());

        String advocateName = null;
        if (c.getAdvocateId() != null) {
            advocateName = advocateRepository.findById(c.getAdvocateId())
                    .map(Advocate::getFullName)
                    .orElse(null);
        }

        int verificationsComplete = (int) verifications.stream()
                .filter(v -> v.getStatus() == VerificationStatus.CLEAN
                        || v.getStatus() == VerificationStatus.ISSUE_FOUND)
                .count();

        return new LegalCaseResponse(
                c.getId(),
                c.getUserId(),
                c.getAdvocateId(),
                advocateName,
                c.getPackageType() != null ? c.getPackageType().name() : null,
                c.getSalePropertyId(),
                c.getPropertyAddress(),
                c.getCity(),
                c.getState(),
                null, // surveyNumber not on entity
                c.getStatus(),
                c.getReportUrl(),
                c.getNotes(),
                c.getFeePaise(),
                c.getPaid(),
                docs.size(),
                verificationsComplete,
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    private LegalVerificationResponse toVerificationResponse(LegalVerification v) {
        return new LegalVerificationResponse(
                v.getId(),
                v.getLegalCaseId(),
                v.getVerificationType() != null ? v.getVerificationType().name() : null,
                v.getStatus() != null ? v.getStatus().name() : null,
                v.getFindings(),
                v.getVerifiedBy(),
                v.getRiskLevel() == RiskLevel.RED,
                v.getRiskLevel() == RiskLevel.RED ? "Issue found during verification" : null,
                v.getVerifiedAt(),
                v.getCreatedAt()
        );
    }

    private ConsultationResponse toConsultationResponse(LegalConsultation c, Advocate advocate) {
        return new ConsultationResponse(
                c.getId(),
                c.getLegalCaseId(),
                c.getAdvocateId(),
                advocate != null ? advocate.getFullName() : null,
                c.getStatus(),
                c.getScheduledAt(),
                c.getDurationMinutes(),
                c.getMeetingLink(),
                c.getConsultationNotes(),
                c.getFeePaise(),
                c.getFeePaise() != null && c.getFeePaise() > 0, // paid placeholder
                c.getCompletedAt(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    private AdvocateResponse toAdvocateResponse(Advocate a) {
        return new AdvocateResponse(
                a.getId(),
                a.getFullName(),
                a.getBarCouncilId(),
                a.getPhone(),
                a.getEmail(),
                a.getCity(),
                a.getState(),
                a.getProfilePhotoUrl(),
                a.getExperienceYears(),
                a.getSpecializations() != null ? List.of(a.getSpecializations()) : List.of(),
                a.getRating(),
                a.getCompletedCases(),
                a.getConsultationFeePaise(),
                a.getActive(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
