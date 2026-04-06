package com.safar.listing.controller;

import com.safar.listing.dto.*;
import com.safar.listing.entity.LoanDocument;
import com.safar.listing.service.HomeLoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/homeloan")
@RequiredArgsConstructor
public class HomeLoanController {

    private final HomeLoanService homeLoanService;

    // ── Check Eligibility ────────────────────────────────────

    @PostMapping("/eligibility")
    public ResponseEntity<EligibilityResponse> checkEligibility(
            @Valid @RequestBody EligibilityRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(homeLoanService.checkEligibility(request, userId));
    }

    // ── Calculate EMI ────────────────────────────────────────

    @PostMapping("/emi/calculate")
    public ResponseEntity<EmiCalculation> calculateEmi(
            @RequestParam Long loanAmountPaise,
            @RequestParam BigDecimal interestRate,
            @RequestParam int tenureMonths) {
        return ResponseEntity.ok(homeLoanService.calculateEmi(loanAmountPaise, interestRate, tenureMonths));
    }

    // ── List Banks ───────────────────────────────────────────

    @GetMapping("/banks")
    public ResponseEntity<List<PartnerBankResponse>> listBanks() {
        return ResponseEntity.ok(homeLoanService.listBanks());
    }

    // ── Compare Banks ────────────────────────────────────────

    @GetMapping("/banks/compare")
    public ResponseEntity<List<PartnerBankResponse>> compareBanks(
            @RequestParam UUID eligibilityId) {
        return ResponseEntity.ok(homeLoanService.compareBanks(eligibilityId));
    }

    // ── Apply Loan ───────────────────────────────────────────

    @PostMapping("/apply")
    public ResponseEntity<LoanApplicationResponse> applyLoan(
            @Valid @RequestBody LoanApplicationRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(homeLoanService.applyLoan(request, userId));
    }

    // ── My Applications ──────────────────────────────────────

    @GetMapping("/applications/my")
    public ResponseEntity<Page<LoanApplicationResponse>> getMyApplications(
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(homeLoanService.getMyApplications(userId, pageable));
    }

    // ── Get Application ──────────────────────────────────────

    @GetMapping("/applications/{id}")
    public ResponseEntity<LoanApplicationResponse> getApplication(@PathVariable UUID id) {
        return ResponseEntity.ok(homeLoanService.getApplication(id));
    }

    // ── Upload Document ──────────────────────────────────────

    @PostMapping("/applications/{id}/documents")
    public ResponseEntity<Void> uploadDocument(
            @PathVariable UUID id,
            @RequestParam String documentType,
            @RequestParam String fileUrl) {
        homeLoanService.uploadDocument(id, documentType, fileUrl);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ── Get Documents ────────────────────────────────────────

    @GetMapping("/applications/{id}/documents")
    public ResponseEntity<List<LoanDocument>> getDocuments(@PathVariable UUID id) {
        return ResponseEntity.ok(homeLoanService.getDocuments(id));
    }
}
