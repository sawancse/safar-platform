package com.safar.user.controller;

import com.safar.user.dto.CaseWorkerDto;
import com.safar.user.dto.CreateOrganizationRequest;
import com.safar.user.dto.OrganizationDto;
import com.safar.user.service.AashrayOrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/aashray")
@RequiredArgsConstructor
public class AashrayOrganizationController {

    private final AashrayOrganizationService aashrayService;

    @PostMapping("/organizations")
    public ResponseEntity<OrganizationDto> createOrganization(@RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(aashrayService.createOrganization(request));
    }

    @GetMapping("/organizations")
    public ResponseEntity<Page<OrganizationDto>> listOrganizations(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(aashrayService.listOrganizations(pageable));
    }

    @GetMapping("/organizations/{id}")
    public ResponseEntity<OrganizationDto> getOrganization(@PathVariable UUID id) {
        return ResponseEntity.ok(aashrayService.getOrganization(id));
    }

    @PostMapping("/organizations/{id}/case-workers")
    public ResponseEntity<CaseWorkerDto> addCaseWorker(@PathVariable UUID id,
                                                        @RequestBody Map<String, UUID> body) {
        UUID userId = body.get("userId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(aashrayService.addCaseWorker(id, userId));
    }

    @GetMapping("/organizations/{id}/case-workers")
    public ResponseEntity<List<CaseWorkerDto>> listCaseWorkers(@PathVariable UUID id) {
        return ResponseEntity.ok(aashrayService.getCaseWorkers(id));
    }

    @DeleteMapping("/case-workers/{id}")
    public ResponseEntity<Void> removeCaseWorker(@PathVariable UUID id) {
        aashrayService.removeCaseWorker(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/organizations/{id}/budget")
    public ResponseEntity<OrganizationDto> addBudget(@PathVariable UUID id,
                                                      @RequestBody Map<String, Long> body) {
        Long amountPaise = body.get("amountPaise");
        return ResponseEntity.ok(aashrayService.addBudget(id, amountPaise));
    }
}
