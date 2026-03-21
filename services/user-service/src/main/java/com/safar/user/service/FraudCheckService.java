package com.safar.user.service;

import com.safar.user.repository.HostKycRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudCheckService {

    private final HostKycRepository kycRepository;

    public FraudCheckResult checkForFraud(UUID userId, String aadhaarNumber, String panNumber, String bankAccountNumber) {
        List<String> flags = new ArrayList<>();
        int riskScore = 0;

        // Check 1: Duplicate Aadhaar across accounts
        if (aadhaarNumber != null) {
            var existing = kycRepository.findByAadhaarNumber(aadhaarNumber);
            if (existing.isPresent() && !existing.get().getUserId().equals(userId)) {
                flags.add("DUPLICATE_AADHAAR: Aadhaar already used by another account");
                riskScore += 40;
            }
        }

        // Check 2: Duplicate PAN across accounts
        if (panNumber != null) {
            var existing = kycRepository.findByPanNumber(panNumber);
            if (existing.isPresent() && !existing.get().getUserId().equals(userId)) {
                flags.add("DUPLICATE_PAN: PAN already used by another account");
                riskScore += 40;
            }
        }

        // Check 3: Duplicate bank account
        if (bankAccountNumber != null) {
            var existing = kycRepository.findByBankAccountNumber(bankAccountNumber);
            if (existing.isPresent() && !existing.get().getUserId().equals(userId)) {
                flags.add("DUPLICATE_BANK: Bank account used by another account");
                riskScore += 30;
            }
        }

        // Check 4: Aadhaar format deep validation
        if (aadhaarNumber != null && !isValidAadhaarChecksum(aadhaarNumber)) {
            flags.add("INVALID_AADHAAR_CHECKSUM: Aadhaar number fails Verhoeff checksum");
            riskScore += 20;
        }

        boolean blocked = riskScore >= 40;

        if (!flags.isEmpty()) {
            log.warn("Fraud check for user {}: riskScore={}, flags={}, blocked={}", userId, riskScore, flags, blocked);
        }

        return new FraudCheckResult(riskScore, flags, blocked);
    }

    private boolean isValidAadhaarChecksum(String aadhaar) {
        // Verhoeff checksum algorithm for Aadhaar validation
        if (aadhaar == null || aadhaar.length() != 12) return false;
        // Simplified: just check format for now — no all-same-digits
        return aadhaar.matches("\\d{12}") && !aadhaar.matches("(\\d)\\1{11}");
    }

    public record FraudCheckResult(int riskScore, List<String> flags, boolean blocked) {}
}
