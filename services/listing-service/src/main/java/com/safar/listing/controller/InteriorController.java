package com.safar.listing.controller;

import com.safar.listing.dto.*;
import com.safar.listing.entity.MaterialSelection;
import com.safar.listing.service.InteriorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interiors")
@RequiredArgsConstructor
public class InteriorController {

    private final InteriorService interiorService;

    // ── Admin: List All Projects ───────────────────────────────

    @GetMapping("/admin/projects")
    public ResponseEntity<Page<InteriorProjectResponse>> getAllProjects(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        if (!"ADMIN".equalsIgnoreCase(role)) throw new org.springframework.security.access.AccessDeniedException("Admin access required");
        return ResponseEntity.ok(interiorService.getAllProjects(status, pageable));
    }

    // ── Admin: Assign Designer ──────────────────────────────

    @PostMapping("/projects/{id}/designer")
    public ResponseEntity<InteriorProjectResponse> assignDesigner(
            @PathVariable UUID id,
            @RequestParam UUID designerId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) throw new org.springframework.security.access.AccessDeniedException("Admin access required");
        return ResponseEntity.ok(interiorService.assignDesigner(id, designerId));
    }

    // ── Book Consultation ────────────────────────────────────

    @PostMapping("/consultation")
    public ResponseEntity<InteriorProjectResponse> bookConsultation(
            @Valid @RequestBody BookConsultationRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interiorService.bookConsultation(request, userId));
    }

    // ── My Projects ──────────────────────────────────────────

    @GetMapping("/projects/my")
    public ResponseEntity<Page<InteriorProjectResponse>> getMyProjects(
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(interiorService.getMyProjects(userId, pageable));
    }

    // ── Get Project ──────────────────────────────────────────

    @GetMapping("/projects/{id}")
    public ResponseEntity<InteriorProjectResponse> getProject(@PathVariable UUID id) {
        return ResponseEntity.ok(interiorService.getProject(id));
    }

    // ── Update Project Status ────────────────────────────────

    @PatchMapping("/projects/{id}/status")
    public ResponseEntity<InteriorProjectResponse> updateProjectStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        return ResponseEntity.ok(interiorService.updateProjectStatus(id, status));
    }

    // ── Add Room Design ──────────────────────────────────────

    @PostMapping("/projects/{id}/rooms")
    public ResponseEntity<RoomDesignResponse> addRoomDesign(
            @PathVariable UUID id,
            @Valid @RequestBody RoomDesignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interiorService.addRoomDesign(id, request));
    }

    // ── Get Room Designs ─────────────────────────────────────

    @GetMapping("/projects/{id}/rooms")
    public ResponseEntity<List<RoomDesignResponse>> getRoomDesigns(@PathVariable UUID id) {
        return ResponseEntity.ok(interiorService.getRoomDesigns(id));
    }

    // ── Approve Room Design ──────────────────────────────────

    @PostMapping("/projects/{projectId}/rooms/{roomId}/approve")
    public ResponseEntity<RoomDesignResponse> approveRoomDesign(
            @PathVariable UUID projectId,
            @PathVariable UUID roomId) {
        return ResponseEntity.ok(interiorService.approveRoomDesign(roomId));
    }

    // ── Add Material Selection ───────────────────────────────

    @PostMapping("/projects/{id}/materials")
    public ResponseEntity<MaterialSelection> addMaterialSelection(
            @PathVariable UUID id,
            @Valid @RequestBody MaterialSelectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interiorService.addMaterialSelection(id, request));
    }

    // ── Get Materials ────────────────────────────────────────

    @GetMapping("/projects/{id}/materials")
    public ResponseEntity<List<MaterialSelection>> getMaterials(@PathVariable UUID id) {
        return ResponseEntity.ok(interiorService.getMaterials(id));
    }

    // ── Browse Catalog ───────────────────────────────────────

    @GetMapping("/materials/catalog")
    public ResponseEntity<List<MaterialCatalogResponse>> browseCatalog(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(interiorService.browseCatalog(category));
    }

    // ── Generate Quote ───────────────────────────────────────

    @PostMapping("/projects/{id}/quote")
    public ResponseEntity<InteriorQuoteResponse> generateQuote(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interiorService.generateQuote(id));
    }

    // ── Approve Quote ────────────────────────────────────────

    @PostMapping("/quotes/{quoteId}/approve")
    public ResponseEntity<InteriorQuoteResponse> approveQuote(@PathVariable UUID quoteId) {
        return ResponseEntity.ok(interiorService.approveQuote(quoteId));
    }

    // ── Add Milestone ────────────────────────────────────────

    @PostMapping("/projects/{id}/milestones")
    public ResponseEntity<MilestoneResponse> addMilestone(
            @PathVariable UUID id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledDate,
            @RequestParam(required = false) Long paymentAmountPaise) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interiorService.addMilestone(id, name, description, scheduledDate, paymentAmountPaise));
    }

    // ── Complete Milestone ───────────────────────────────────

    @PostMapping("/milestones/{milestoneId}/complete")
    public ResponseEntity<MilestoneResponse> completeMilestone(
            @PathVariable UUID milestoneId,
            @RequestParam(required = false) String[] photos) {
        return ResponseEntity.ok(interiorService.completeMilestone(milestoneId, photos));
    }

    // ── Get Milestones ───────────────────────────────────────

    @GetMapping("/projects/{id}/milestones")
    public ResponseEntity<List<MilestoneResponse>> getMilestones(@PathVariable UUID id) {
        return ResponseEntity.ok(interiorService.getMilestones(id));
    }

    // ── Add Quality Check ────────────────────────────────────

    @PostMapping("/projects/{id}/quality-checks")
    public ResponseEntity<QualityCheckResponse> addQualityCheck(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID milestoneId,
            @RequestParam String checkpointName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interiorService.addQualityCheck(id, milestoneId, checkpointName, category, status, notes));
    }

    // ── Get Quality Checks ───────────────────────────────────

    @GetMapping("/projects/{id}/quality-checks")
    public ResponseEntity<List<QualityCheckResponse>> getQualityChecks(@PathVariable UUID id) {
        return ResponseEntity.ok(interiorService.getQualityChecks(id));
    }

    // Designers CRUD moved to ProfessionalController

    // ── Submit Review ────────────────────────────────────────

    @PostMapping("/projects/{id}/review")
    public ResponseEntity<Void> submitReview(
            @PathVariable UUID id,
            @RequestParam String feedback,
            @RequestParam Integer rating,
            @RequestHeader("X-User-Id") UUID userId) {
        interiorService.submitReview(id, feedback, rating, userId);
        return ResponseEntity.ok().build();
    }
}
