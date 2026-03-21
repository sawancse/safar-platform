package com.safar.listing.controller;

import com.safar.listing.dto.HospitalProcedureDto;
import com.safar.listing.dto.MedicalCostEstimate;
import com.safar.listing.dto.MedicalPackageRequest;
import com.safar.listing.dto.MedicalStayPackageDto;
import com.safar.listing.entity.HospitalPartner;
import com.safar.listing.entity.MedicalStayPackage;
import com.safar.listing.service.MedicalStayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medical-stay")
@RequiredArgsConstructor
public class MedicalStayController {

    private final MedicalStayService medicalStayService;

    @GetMapping("/search")
    public ResponseEntity<List<MedicalStayPackageDto>> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String specialty) {
        return ResponseEntity.ok(medicalStayService.searchPackages(city, specialty));
    }

    @GetMapping("/hospitals")
    public ResponseEntity<Page<HospitalPartner>> getHospitals(
            @RequestParam(required = false) String city,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(medicalStayService.getHospitalsByCity(city, pageable));
    }

    // ── Procedures ───────────────────────────────────────────

    @GetMapping("/hospitals/{hospitalId}/procedures")
    public ResponseEntity<List<HospitalProcedureDto>> getHospitalProcedures(
            @PathVariable UUID hospitalId) {
        return ResponseEntity.ok(medicalStayService.getProcedures(hospitalId));
    }

    @GetMapping("/procedures/search")
    public ResponseEntity<List<HospitalProcedureDto>> searchProcedures(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String specialty) {
        if (query != null && !query.isBlank()) {
            return ResponseEntity.ok(medicalStayService.searchProcedures(query));
        } else if (specialty != null && !specialty.isBlank()) {
            return ResponseEntity.ok(medicalStayService.getProceduresBySpecialty(specialty));
        }
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/hospitals/{hospitalId}/procedures")
    public ResponseEntity<HospitalProcedureDto> addProcedure(
            @PathVariable UUID hospitalId,
            @RequestBody HospitalProcedureDto req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(medicalStayService.addProcedure(hospitalId, req));
    }

    // ── Cost Estimate ────────────────────────────────────────

    @GetMapping("/estimate")
    public ResponseEntity<MedicalCostEstimate> estimateCost(
            @RequestParam UUID procedureId,
            @RequestParam UUID packageId) {
        return ResponseEntity.ok(medicalStayService.estimateCost(procedureId, packageId));
    }

    @PostMapping("/listings/{id}/package")
    public ResponseEntity<MedicalStayPackage> registerPackage(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestBody MedicalPackageRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(medicalStayService.registerPackage(hostId, id, req));
    }
}
