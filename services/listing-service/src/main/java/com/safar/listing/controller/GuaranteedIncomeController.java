package com.safar.listing.controller;

import com.safar.listing.dto.GuaranteeRequest;
import com.safar.listing.entity.GuaranteedIncomeContract;
import com.safar.listing.service.GuaranteedIncomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class GuaranteedIncomeController {

    private final GuaranteedIncomeService guaranteedIncomeService;

    @PostMapping("/api/v1/listings/{id}/guaranteed-income")
    public ResponseEntity<GuaranteedIncomeContract> create(
            Authentication auth,
            @PathVariable UUID id,
            @RequestBody GuaranteeRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(guaranteedIncomeService.createContract(hostId, id, req));
    }

    @GetMapping("/api/v1/guaranteed-income/contracts")
    public ResponseEntity<List<GuaranteedIncomeContract>> getContracts(Authentication auth) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(guaranteedIncomeService.getContracts(hostId));
    }
}
