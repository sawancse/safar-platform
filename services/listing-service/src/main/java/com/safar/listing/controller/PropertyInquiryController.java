package com.safar.listing.controller;

import com.safar.listing.dto.CreateInquiryRequest;
import com.safar.listing.dto.InquiryResponse;
import com.safar.listing.entity.enums.InquiryStatus;
import com.safar.listing.service.PropertyInquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class PropertyInquiryController {

    private final PropertyInquiryService inquiryService;

    @PostMapping
    public ResponseEntity<InquiryResponse> create(
            @Valid @RequestBody CreateInquiryRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inquiryService.create(request, userId));
    }

    @GetMapping("/buyer")
    public ResponseEntity<Page<InquiryResponse>> getBuyerInquiries(
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(inquiryService.getBuyerInquiries(userId, pageable));
    }

    @GetMapping("/seller")
    public ResponseEntity<Page<InquiryResponse>> getSellerInquiries(
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(inquiryService.getSellerInquiries(userId, pageable));
    }

    @GetMapping("/property/{salePropertyId}")
    public ResponseEntity<Page<InquiryResponse>> getPropertyInquiries(
            @PathVariable UUID salePropertyId,
            Pageable pageable) {
        return ResponseEntity.ok(inquiryService.getPropertyInquiries(salePropertyId, pageable));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<InquiryResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam InquiryStatus status,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(inquiryService.updateStatus(id, status, userId));
    }

    @PatchMapping("/{id}/note")
    public ResponseEntity<InquiryResponse> addNote(
            @PathVariable UUID id,
            @RequestBody String note,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(inquiryService.addNote(id, note, userId));
    }
}
