package com.safar.user.service;

import com.safar.user.dto.*;
import com.safar.user.entity.HostKyc;
import com.safar.user.entity.enums.KycStatus;
import com.safar.user.repository.HostKycRepository;
import com.safar.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HostKycService {

    private final HostKycRepository kycRepository;
    private final FraudCheckService fraudCheckService;
    private final TrustScoreService trustScoreService;
    private final ProfileRepository profileRepository;

    public HostKycDto getKyc(UUID userId) {
        HostKyc kyc = kycRepository.findByUserId(userId)
                .orElseGet(() -> {
                    HostKyc created = HostKyc.builder().userId(userId).build();
                    return kycRepository.save(created);
                });
        return toDto(kyc);
    }

    @Transactional
    public HostKycDto updateIdentity(UUID userId, UpdateKycIdentityRequest req) {
        // Run fraud check before saving
        FraudCheckService.FraudCheckResult fraudResult = fraudCheckService.checkForFraud(
                userId, req.aadhaarNumber(), req.panNumber(), null);
        if (fraudResult.blocked()) {
            throw new IllegalStateException("Verification blocked: " + String.join("; ", fraudResult.flags()));
        }
        // Update fraud risk score on profile
        profileRepository.findById(userId).ifPresent(profile -> {
            profile.setFraudRiskScore(fraudResult.riskScore());
            profileRepository.save(profile);
        });

        HostKyc kyc = getOrCreate(userId);
        kyc.setFullLegalName(req.fullLegalName());
        kyc.setDateOfBirth(req.dateOfBirth());
        kyc.setAadhaarNumber(req.aadhaarNumber());
        kyc.setPanNumber(req.panNumber());
        // Auto-verify in dev mode (replace with actual API verification in prod)
        kyc.setAadhaarVerified(true);
        kyc.setPanVerified(true);
        if (kyc.getStatus() == KycStatus.NOT_STARTED) {
            kyc.setStatus(KycStatus.ADDRESS_PENDING);
        }
        HostKycDto dto = toDto(kycRepository.save(kyc));
        // Recalculate trust score after identity update
        trustScoreService.calculateTrustScore(userId);
        return dto;
    }

    @Transactional
    public HostKycDto updateAddress(UUID userId, UpdateKycAddressRequest req) {
        HostKyc kyc = getOrCreate(userId);
        kyc.setAddressLine1(req.addressLine1());
        kyc.setAddressLine2(req.addressLine2());
        kyc.setCity(req.city());
        kyc.setState(req.state());
        kyc.setPincode(req.pincode());
        if (kyc.getStatus() == KycStatus.ADDRESS_PENDING || kyc.getStatus() == KycStatus.NOT_STARTED) {
            kyc.setStatus(KycStatus.BANK_PENDING);
        }
        return toDto(kycRepository.save(kyc));
    }

    @Transactional
    public HostKycDto updateBank(UUID userId, UpdateKycBankRequest req) {
        // Run fraud check for duplicate bank account
        FraudCheckService.FraudCheckResult fraudResult = fraudCheckService.checkForFraud(
                userId, null, null, req.bankAccountNumber());
        if (fraudResult.blocked()) {
            throw new IllegalStateException("Verification blocked: " + String.join("; ", fraudResult.flags()));
        }
        // Update fraud risk score on profile
        profileRepository.findById(userId).ifPresent(profile -> {
            profile.setFraudRiskScore(Math.max(profile.getFraudRiskScore(), fraudResult.riskScore()));
            profileRepository.save(profile);
        });

        HostKyc kyc = getOrCreate(userId);
        kyc.setBankAccountName(req.bankAccountName());
        kyc.setBankAccountNumber(req.bankAccountNumber());
        kyc.setBankIfsc(req.bankIfsc());
        kyc.setBankName(req.bankName());
        // Auto-verify in dev mode
        kyc.setBankVerified(true);
        if (kyc.getStatus() == KycStatus.BANK_PENDING || kyc.getStatus() == KycStatus.NOT_STARTED) {
            kyc.setStatus(KycStatus.SUBMITTED);
            kyc.setSubmittedAt(OffsetDateTime.now());
        }
        HostKycDto dto = toDto(kycRepository.save(kyc));
        // Recalculate trust score after bank update
        trustScoreService.calculateTrustScore(userId);
        return dto;
    }

    @Transactional
    public HostKycDto updateBusiness(UUID userId, UpdateKycBusinessRequest req) {
        HostKyc kyc = getOrCreate(userId);
        kyc.setGstin(req.gstin());
        kyc.setBusinessName(req.businessName());
        kyc.setBusinessType(req.businessType());
        if (req.gstin() != null && !req.gstin().isBlank()) {
            kyc.setGstVerified(true); // auto-verify in dev
        }
        return toDto(kycRepository.save(kyc));
    }

    @Transactional
    public HostKycDto updateDocuments(UUID userId, String aadhaarFrontUrl, String aadhaarBackUrl,
                                       String panUrl, String selfieUrl) {
        HostKyc kyc = getOrCreate(userId);
        if (aadhaarFrontUrl != null) kyc.setAadhaarFrontUrl(aadhaarFrontUrl);
        if (aadhaarBackUrl != null) kyc.setAadhaarBackUrl(aadhaarBackUrl);
        if (panUrl != null) kyc.setPanUrl(panUrl);
        if (selfieUrl != null) kyc.setSelfieUrl(selfieUrl);
        log.info("Host KYC documents updated for user {}", userId);
        return toDto(kycRepository.save(kyc));
    }

    @Transactional
    public HostKycDto submit(UUID userId) {
        HostKyc kyc = getOrCreate(userId);
        if (kyc.getFullLegalName() == null || kyc.getAadhaarNumber() == null || kyc.getPanNumber() == null) {
            throw new IllegalStateException("Identity details are required before submitting KYC");
        }
        if (kyc.getAadhaarFrontUrl() == null || kyc.getAadhaarBackUrl() == null) {
            throw new IllegalStateException("Aadhaar document images (front and back) are required");
        }
        if (kyc.getPanUrl() == null) {
            throw new IllegalStateException("PAN card image is required");
        }
        if (kyc.getSelfieUrl() == null) {
            throw new IllegalStateException("Selfie photo is required for identity verification");
        }
        if (kyc.getBankAccountNumber() == null || kyc.getBankIfsc() == null) {
            throw new IllegalStateException("Bank details are required before submitting KYC");
        }
        kyc.setStatus(KycStatus.SUBMITTED);
        kyc.setSubmittedAt(OffsetDateTime.now());
        log.info("Host KYC submitted for user {}", userId);
        return toDto(kycRepository.save(kyc));
    }

    // Admin endpoints
    @Transactional
    public HostKycDto approve(UUID kycId, UUID adminId) {
        HostKyc kyc = kycRepository.findById(kycId)
                .orElseThrow(() -> new java.util.NoSuchElementException("KYC not found"));
        kyc.setStatus(KycStatus.VERIFIED);
        kyc.setVerifiedAt(OffsetDateTime.now());
        kyc.setVerifiedBy(adminId);
        kyc.setRejectedAt(null);
        kyc.setRejectionReason(null);
        log.info("Host KYC {} approved by admin {}", kycId, adminId);
        HostKycDto dto = toDto(kycRepository.save(kyc));
        // Recalculate trust score after KYC approval
        trustScoreService.calculateTrustScore(kyc.getUserId());
        return dto;
    }

    @Transactional
    public HostKycDto reject(UUID kycId, UUID adminId, String reason) {
        HostKyc kyc = kycRepository.findById(kycId)
                .orElseThrow(() -> new java.util.NoSuchElementException("KYC not found"));
        kyc.setStatus(KycStatus.REJECTED);
        kyc.setRejectedAt(OffsetDateTime.now());
        kyc.setRejectionReason(reason);
        kyc.setVerifiedBy(adminId);
        log.info("Host KYC {} rejected by admin {}: {}", kycId, adminId, reason);
        return toDto(kycRepository.save(kyc));
    }

    public List<HostKycDto> getPendingKycs() {
        return kycRepository.findByStatus(KycStatus.SUBMITTED).stream()
                .map(this::toDto).toList();
    }

    public List<HostKycDto> getAllKycs(String status) {
        if (status != null && !status.isBlank()) {
            KycStatus kycStatus = KycStatus.valueOf(status);
            return kycRepository.findByStatus(kycStatus).stream()
                    .map(this::toDto).toList();
        }
        return kycRepository.findByStatusNot(KycStatus.NOT_STARTED).stream()
                .map(this::toDto).toList();
    }

    public KycAdminDetailDto getKycAdminDetail(UUID kycId) {
        HostKyc kyc = kycRepository.findById(kycId)
                .orElseThrow(() -> new java.util.NoSuchElementException("KYC not found: " + kycId));

        var profile = profileRepository.findById(kyc.getUserId()).orElse(null);

        int trustScore = profile != null && profile.getTrustScore() != null ? profile.getTrustScore() : 0;
        String trustBadge = trustScoreService.getTrustBadge(trustScore);
        String verificationLevel = profile != null && profile.getVerificationLevel() != null
                ? profile.getVerificationLevel() : "UNVERIFIED";
        int fraudRiskScore = profile != null && profile.getFraudRiskScore() != null
                ? profile.getFraudRiskScore() : 0;
        String hostType = profile != null && profile.getHostType() != null
                ? profile.getHostType() : "INDIVIDUAL";
        String residentStatus = profile != null && profile.getResidentStatus() != null
                ? profile.getResidentStatus() : "RESIDENT";

        // Run live fraud check
        FraudCheckService.FraudCheckResult fraudResult = fraudCheckService.checkForFraud(
                kyc.getUserId(), kyc.getAadhaarNumber(), kyc.getPanNumber(), kyc.getBankAccountNumber());

        return new KycAdminDetailDto(
                toDto(kyc), trustScore, trustBadge, verificationLevel,
                fraudRiskScore, fraudResult.flags(), hostType, residentStatus
        );
    }

    @Transactional
    public List<HostKycDto> bulkApprove(List<UUID> kycIds, UUID adminId) {
        return kycIds.stream().map(kycId -> approve(kycId, adminId)).toList();
    }

    @Transactional
    public List<HostKyc> checkExpiredVerifications() {
        List<HostKyc> verified = kycRepository.findByStatus(KycStatus.VERIFIED);
        List<HostKyc> expired = new ArrayList<>();
        OffsetDateTime oneYearAgo = OffsetDateTime.now().minusYears(1);

        for (HostKyc kyc : verified) {
            if (kyc.getVerifiedAt() != null && kyc.getVerifiedAt().isBefore(oneYearAgo)) {
                kyc.setStatus(KycStatus.SUBMITTED); // reset to require re-verification
                kyc.setBankVerified(false); // bank needs re-verification annually
                kycRepository.save(kyc);
                expired.add(kyc);
                log.info("KYC expired for user {}, reset to SUBMITTED for re-verification", kyc.getUserId());
            }
        }
        return expired;
    }

    private HostKyc getOrCreate(UUID userId) {
        return kycRepository.findByUserId(userId)
                .orElseGet(() -> kycRepository.save(HostKyc.builder().userId(userId).build()));
    }

    private int calcCompletion(HostKyc k) {
        int pct = 0;
        if (k.getFullLegalName() != null && k.getAadhaarNumber() != null && k.getPanNumber() != null) pct += 30;
        if (k.getAddressLine1() != null && k.getCity() != null && k.getPincode() != null) pct += 20;
        if (k.getBankAccountNumber() != null && k.getBankIfsc() != null) pct += 30;
        if (k.getStatus() == KycStatus.SUBMITTED) pct += 10;
        if (k.getStatus() == KycStatus.VERIFIED) pct = 100;
        if (k.getGstin() != null && !k.getGstin().isBlank()) pct += 10;
        return Math.min(100, pct);
    }

    private HostKycDto toDto(HostKyc k) {
        // Mask sensitive numbers for display
        String maskedAadhaar = k.getAadhaarNumber() != null && k.getAadhaarNumber().length() == 12
                ? "XXXX XXXX " + k.getAadhaarNumber().substring(8) : k.getAadhaarNumber();
        String maskedAccount = k.getBankAccountNumber() != null && k.getBankAccountNumber().length() > 4
                ? "XXXX" + k.getBankAccountNumber().substring(k.getBankAccountNumber().length() - 4)
                : k.getBankAccountNumber();

        return new HostKycDto(
                k.getId(), k.getUserId(), k.getStatus().name(),
                k.getFullLegalName(), k.getDateOfBirth(),
                maskedAadhaar, k.getAadhaarVerified(),
                k.getPanNumber(), k.getPanVerified(),
                k.getAadhaarFrontUrl(), k.getAadhaarBackUrl(),
                k.getPanUrl(), k.getSelfieUrl(),
                k.getAddressLine1(), k.getAddressLine2(),
                k.getCity(), k.getState(), k.getPincode(),
                k.getBankAccountName(), maskedAccount,
                k.getBankIfsc(), k.getBankName(), k.getBankVerified(),
                k.getGstin(), k.getGstVerified(),
                k.getBusinessName(), k.getBusinessType(),
                k.getSubmittedAt(), k.getVerifiedAt(), k.getRejectedAt(),
                k.getRejectionReason(), calcCompletion(k)
        );
    }
}
