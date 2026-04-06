package com.safar.listing.controller;

import com.safar.listing.dto.*;
import com.safar.listing.service.BuilderProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/builder-projects")
@RequiredArgsConstructor
public class BuilderProjectController {

    private final BuilderProjectService projectService;

    // ── Browse (public) ─────────────────────────────────────

    @GetMapping
    public ResponseEntity<Page<BuilderProjectResponse>> browse(
            @RequestParam(required = false) String city,
            Pageable pageable) {
        return ResponseEntity.ok(projectService.browseProjects(city, pageable));
    }

    // ── Project CRUD ──────────────────────────────────────────

    @PostMapping
    public ResponseEntity<BuilderProjectResponse> create(
            @Valid @RequestBody CreateBuilderProjectRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BuilderProjectResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BuilderProjectResponse> update(
            @PathVariable UUID id,
            @RequestBody CreateBuilderProjectRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(projectService.updateProject(id, request, userId));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<BuilderProjectResponse> publish(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(projectService.publish(id, userId));
    }

    @GetMapping("/my-projects")
    public ResponseEntity<Page<BuilderProjectResponse>> getMyProjects(
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(projectService.getBuilderProjects(userId, pageable));
    }

    // ── Unit Types ────────────────────────────────────────────

    @PostMapping("/{projectId}/unit-types")
    public ResponseEntity<UnitTypeResponse> addUnitType(
            @PathVariable UUID projectId,
            @Valid @RequestBody UnitTypeRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.addUnitType(projectId, request, userId));
    }

    @GetMapping("/{projectId}/unit-types")
    public ResponseEntity<List<UnitTypeResponse>> getUnitTypes(@PathVariable UUID projectId) {
        return ResponseEntity.ok(projectService.getUnitTypes(projectId));
    }

    @DeleteMapping("/unit-types/{unitTypeId}")
    public ResponseEntity<Void> deleteUnitType(
            @PathVariable UUID unitTypeId,
            @RequestHeader("X-User-Id") UUID userId) {
        projectService.deleteUnitType(unitTypeId, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Price Calculator ──────────────────────────────────────

    @GetMapping("/unit-types/{unitTypeId}/calculate-price")
    public ResponseEntity<UnitPriceCalculation> calculatePrice(
            @PathVariable UUID unitTypeId,
            @RequestParam(defaultValue = "1") int floor,
            @RequestParam(defaultValue = "false") boolean preferredFacing) {
        return ResponseEntity.ok(projectService.calculateUnitPrice(unitTypeId, floor, preferredFacing));
    }

    // ── Construction Updates ──────────────────────────────────

    @PostMapping("/{projectId}/construction-updates")
    public ResponseEntity<ConstructionUpdateResponse> addConstructionUpdate(
            @PathVariable UUID projectId,
            @Valid @RequestBody ConstructionUpdateRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.addConstructionUpdate(projectId, request, userId));
    }

    @GetMapping("/{projectId}/construction-updates")
    public ResponseEntity<List<ConstructionUpdateResponse>> getConstructionUpdates(@PathVariable UUID projectId) {
        return ResponseEntity.ok(projectService.getConstructionUpdates(projectId));
    }

    // ── Admin ─────────────────────────────────────────────────

    @GetMapping("/admin/list")
    public ResponseEntity<Page<Map<String, Object>>> adminList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            Pageable pageable) {
        return ResponseEntity.ok(projectService.adminList(status, city, state, locality, search, verified, dateFrom, dateTo, pageable));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<BuilderProjectResponse> adminVerify(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.adminVerify(id));
    }

    @PostMapping("/{id}/verify-rera")
    public ResponseEntity<BuilderProjectResponse> adminVerifyRera(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.adminVerifyRera(id));
    }

    @PostMapping("/admin/reindex")
    public ResponseEntity<Integer> adminReindex() {
        return ResponseEntity.ok(projectService.reindexAll());
    }
}
