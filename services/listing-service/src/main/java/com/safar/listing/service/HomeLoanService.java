package com.safar.listing.service;

import com.safar.listing.dto.*;
import com.safar.listing.entity.LoanApplication;
import com.safar.listing.entity.LoanDocument;
import com.safar.listing.entity.LoanEligibility;
import com.safar.listing.entity.PartnerBank;
import com.safar.listing.entity.enums.EmploymentType;
import com.safar.listing.entity.enums.LoanApplicationStatus;
import com.safar.listing.entity.enums.LoanDocumentType;
import com.safar.listing.repository.LoanApplicationRepository;
import com.safar.listing.repository.LoanDocumentRepository;
import com.safar.listing.repository.LoanEligibilityRepository;
import com.safar.listing.repository.PartnerBankRepository;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeLoanService {

    private final LoanEligibilityRepository loanEligibilityRepository;
    private final PartnerBankRepository partnerBankRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanDocumentRepository loanDocumentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ── Check Eligibility ────────────────────────────────────

    @Transactional
    public EligibilityResponse checkEligibility(EligibilityRequest req, UUID userId) {
        long monthlyIncome = req.monthlyIncomePaise();
        long currentEmis = req.currentEmisPaise() != null ? req.currentEmisPaise() : 0L;
        int tenureMonths = req.desiredTenureMonths() != null ? req.desiredTenureMonths() : 240;

        // Max eligible = (monthlyIncome - currentEMIs) * 0.5 * tenureMonths * 0.85
        long disposable = monthlyIncome - currentEmis;
        long maxEligible = (long) (disposable * 0.5 * tenureMonths * 0.85);
        long maxEmi = (long) (disposable * 0.5);

        boolean eligible = req.desiredLoanAmountPaise() == null
                || req.desiredLoanAmountPaise() <= maxEligible;

        LoanEligibility entity = LoanEligibility.builder()
                .userId(userId)
                .employmentType(EmploymentType.valueOf(req.employmentType()))
                .monthlyIncomePaise(monthlyIncome)
                .existingEmiPaise(currentEmis)
                .eligibleAmountPaise(maxEligible)
                .estimatedEmiPaise(maxEmi)
                .offeredTenureMonths(tenureMonths)
                .calculatedAt(OffsetDateTime.now())
                .build();

        entity = loanEligibilityRepository.save(entity);
        log.info("Eligibility calculated for user {}: maxEligible={} paise", userId, maxEligible);

        return new EligibilityResponse(
                entity.getId(),
                maxEligible,
                maxEmi,
                req.desiredLoanAmountPaise(),
                tenureMonths,
                eligible
        );
    }

    // ── Calculate EMI ────────────────────────────────────────

    public EmiCalculation calculateEmi(Long loanAmountPaise, BigDecimal interestRate, int tenureMonths) {
        // Standard EMI formula: P*r*(1+r)^n / ((1+r)^n - 1)
        double principal = loanAmountPaise;
        double monthlyRate = interestRate.doubleValue() / 12.0 / 100.0;
        int n = tenureMonths;

        long emiPaise;
        if (monthlyRate == 0) {
            emiPaise = loanAmountPaise / n;
        } else {
            double factor = Math.pow(1 + monthlyRate, n);
            double emi = principal * monthlyRate * factor / (factor - 1);
            emiPaise = Math.round(emi);
        }

        long totalAmount = emiPaise * n;
        long totalInterest = totalAmount - loanAmountPaise;

        return new EmiCalculation(
                loanAmountPaise,
                interestRate,
                tenureMonths,
                emiPaise,
                totalInterest,
                totalAmount
        );
    }

    // ── List Banks ───────────────────────────────────────────

    public List<PartnerBankResponse> listBanks() {
        return partnerBankRepository.findByActiveTrueOrderByInterestRateMinAsc()
                .stream()
                .map(this::toBankResponse)
                .toList();
    }

    // ── Compare Banks ────────────────────────────────────────

    public List<PartnerBankResponse> compareBanks(UUID eligibilityId) {
        LoanEligibility eligibility = loanEligibilityRepository.findById(eligibilityId)
                .orElseThrow(() -> new RuntimeException("Eligibility not found: " + eligibilityId));

        long eligibleAmount = eligibility.getEligibleAmountPaise() != null
                ? eligibility.getEligibleAmountPaise() : 0L;
        long monthlyIncome = eligibility.getMonthlyIncomePaise() != null
                ? eligibility.getMonthlyIncomePaise() : 0L;

        return partnerBankRepository.findByActiveTrueOrderByInterestRateMinAsc()
                .stream()
                .filter(bank -> {
                    boolean amountOk = bank.getMaxLoanAmountPaise() == null
                            || eligibleAmount <= bank.getMaxLoanAmountPaise();
                    return amountOk;
                })
                .map(this::toBankResponse)
                .toList();
    }

    // ── Apply Loan ───────────────────────────────────��───────

    @Transactional
    public LoanApplicationResponse applyLoan(LoanApplicationRequest req, UUID userId) {
        PartnerBank bank = partnerBankRepository.findById(req.bankId())
                .orElseThrow(() -> new RuntimeException("Bank not found: " + req.bankId()));

        LoanApplication application = LoanApplication.builder()
                .userId(userId)
                .partnerBankId(req.bankId())
                .requestedAmountPaise(req.loanAmountPaise())
                .requestedTenureMonths(req.tenureMonths())
                .salePropertyId(req.propertyId())
                .applicantName(req.applicantName())
                .applicantPhone(req.applicantPhone())
                .applicantEmail(req.applicantEmail())
                .status(LoanApplicationStatus.APPLIED)
                .applicationNumber("SL" + System.currentTimeMillis())
                .build();

        application = loanApplicationRepository.save(application);
        log.info("Loan application created: {} by user {} for bank {}", application.getId(), userId, req.bankId());

        kafkaTemplate.send("homeloan.applied", application.getId().toString(), application.getId().toString());

        return toApplicationResponse(application, bank);
    }

    // ── My Applications ──────────────────────────────────────

    public Page<LoanApplicationResponse> getMyApplications(UUID userId, Pageable pageable) {
        return loanApplicationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(app -> {
                    PartnerBank bank = app.getPartnerBankId() != null
                            ? partnerBankRepository.findById(app.getPartnerBankId()).orElse(null)
                            : null;
                    return toApplicationResponse(app, bank);
                });
    }

    // ── Get Application ──────────────────────────────────────

    public LoanApplicationResponse getApplication(UUID id) {
        LoanApplication app = loanApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found: " + id));
        PartnerBank bank = app.getPartnerBankId() != null
                ? partnerBankRepository.findById(app.getPartnerBankId()).orElse(null)
                : null;
        return toApplicationResponse(app, bank);
    }

    // ── Upload Document ──────────────────────────────────────

    @Transactional
    public void uploadDocument(UUID applicationId, String documentType, String fileUrl) {
        loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        LoanDocument doc = LoanDocument.builder()
                .loanApplicationId(applicationId)
                .documentType(LoanDocumentType.valueOf(documentType))
                .fileName(documentType + "_" + System.currentTimeMillis())
                .fileUrl(fileUrl)
                .build();

        loanDocumentRepository.save(doc);
        log.info("Document uploaded for application {}: type={}", applicationId, documentType);
    }

    // ── Get Documents ────────────────────────────────────────

    public List<LoanDocument> getDocuments(UUID applicationId) {
        return loanDocumentRepository.findByLoanApplicationId(applicationId);
    }

    // ── Update Application Status (Admin) ────────────────────

    @Transactional
    public LoanApplicationResponse updateApplicationStatus(UUID id, LoanApplicationStatus status) {
        LoanApplication app = loanApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found: " + id));

        app.setStatus(status);

        if (status == LoanApplicationStatus.SANCTIONED) {
            app.setSanctionedAt(OffsetDateTime.now());
        } else if (status == LoanApplicationStatus.DISBURSED) {
            app.setDisbursedAt(OffsetDateTime.now());
        }

        app = loanApplicationRepository.save(app);
        log.info("Loan application {} status updated to {}", id, status);

        kafkaTemplate.send("homeloan.status.updated", id.toString(), status.name());

        PartnerBank bank = app.getPartnerBankId() != null
                ? partnerBankRepository.findById(app.getPartnerBankId()).orElse(null)
                : null;
        return toApplicationResponse(app, bank);
    }

    // ── Private Helpers ──────────────────────────────────────

    private PartnerBankResponse toBankResponse(PartnerBank b) {
        return new PartnerBankResponse(
                b.getId(),
                b.getBankName(),
                null, // bankCode
                b.getLogoUrl(),
                b.getInterestRateMin(),
                b.getInterestRateMax(),
                b.getMaxLoanAmountPaise(),
                b.getMaxTenureMonths(),
                b.getProcessingFeePercent(),
                b.getProcessingFeeMaxPaise(),
                b.getPreApprovalAvailable(),
                b.getBalanceTransferAvailable(),
                b.getActive(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }

    private LoanApplicationResponse toApplicationResponse(LoanApplication app, PartnerBank bank) {
        return new LoanApplicationResponse(
                app.getId(),
                app.getUserId(),
                app.getPartnerBankId(),
                bank != null ? bank.getBankName() : null,
                bank != null ? bank.getLogoUrl() : null,
                app.getRequestedAmountPaise(),
                app.getRequestedTenureMonths(),
                app.getSanctionedInterestRate(),
                app.getEstimatedEmiPaise(),
                app.getSanctionedAmountPaise(),
                app.getSalePropertyId(),
                app.getApplicantName(),
                app.getApplicantPhone(),
                app.getApplicantEmail(),
                app.getStatus(),
                app.getRemarks(),
                app.getApplicationNumber(),
                app.getCreatedAt(),
                app.getSanctionedAt(),
                app.getDisbursedAt(),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }
}
