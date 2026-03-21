package com.safar.listing.controller;

import com.safar.listing.dto.ManagedExpenseRequest;
import com.safar.listing.entity.ManagedStayExpense;
import com.safar.listing.service.ManagedStayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/managed-stay/{contractId}/expenses")
@RequiredArgsConstructor
public class AdminManagedStayController {

    private final ManagedStayService managedStayService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ManagedStayExpense> recordExpense(
            @PathVariable UUID contractId,
            @RequestBody ManagedExpenseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(managedStayService.recordExpense(contractId, req));
    }
}
