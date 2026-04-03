package com.safar.booking.controller;

import com.safar.booking.dto.AddDeductionRequest;
import com.safar.booking.dto.InitiateSettlementRequest;
import com.safar.booking.dto.SettlementResponse;
import com.safar.booking.entity.SettlementDeduction;
import com.safar.booking.entity.TenancySettlement;
import com.safar.booking.service.TenancySettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pg-tenancies/{tenancyId}/settlement")
@RequiredArgsConstructor
public class TenancySettlementController {

    private final TenancySettlementService settlementService;

    @PostMapping
    public ResponseEntity<SettlementResponse> initiateSettlement(
            @PathVariable UUID tenancyId,
            @RequestBody InitiateSettlementRequest request) {
        TenancySettlement settlement = settlementService.initiateSettlement(tenancyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(settlementService.toResponse(settlement));
    }

    @GetMapping
    public ResponseEntity<SettlementResponse> getSettlement(@PathVariable UUID tenancyId) {
        TenancySettlement settlement = settlementService.getByTenancyId(tenancyId);
        return ResponseEntity.ok(settlementService.toResponse(settlement));
    }

    @PostMapping("/deductions")
    public ResponseEntity<Map<String, Object>> addDeduction(
            @PathVariable UUID tenancyId,
            @RequestBody AddDeductionRequest request) {
        SettlementDeduction deduction = settlementService.addDeduction(tenancyId, request);
        TenancySettlement updated = settlementService.getByTenancyId(tenancyId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "deductionId", deduction.getId(),
                "settlement", settlementService.toResponse(updated)
        ));
    }

    @DeleteMapping("/deductions/{deductionId}")
    public ResponseEntity<SettlementResponse> removeDeduction(
            @PathVariable UUID tenancyId,
            @PathVariable UUID deductionId) {
        settlementService.removeDeduction(tenancyId, deductionId);
        TenancySettlement updated = settlementService.getByTenancyId(tenancyId);
        return ResponseEntity.ok(settlementService.toResponse(updated));
    }

    @PostMapping("/inspection")
    public ResponseEntity<SettlementResponse> completeInspection(
            @PathVariable UUID tenancyId,
            @RequestBody Map<String, String> body) {
        String notes = body.getOrDefault("inspectionNotes", "");
        TenancySettlement settlement = settlementService.completeInspection(tenancyId, notes);
        return ResponseEntity.ok(settlementService.toResponse(settlement));
    }

    @PostMapping("/approve")
    public ResponseEntity<SettlementResponse> approveSettlement(
            @PathVariable UUID tenancyId,
            @RequestHeader(value = "X-User-Role", defaultValue = "TENANT") String role) {
        TenancySettlement settlement = settlementService.approveSettlement(tenancyId, role);
        return ResponseEntity.ok(settlementService.toResponse(settlement));
    }

    @PostMapping("/process-refund")
    public ResponseEntity<SettlementResponse> processRefund(
            @PathVariable UUID tenancyId,
            @RequestParam UUID hostId,
            @RequestParam(required = false) String upiId) {
        TenancySettlement settlement = settlementService.processRefund(tenancyId, hostId, upiId);
        return ResponseEntity.ok(settlementService.toResponse(settlement));
    }

    @PostMapping("/mark-settled")
    public ResponseEntity<SettlementResponse> markSettled(
            @PathVariable UUID tenancyId,
            @RequestParam(required = false) String razorpayRefundId) {
        TenancySettlement settlement = settlementService.markSettled(tenancyId, razorpayRefundId);
        return ResponseEntity.ok(settlementService.toResponse(settlement));
    }
}
