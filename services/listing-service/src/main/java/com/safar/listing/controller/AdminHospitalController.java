package com.safar.listing.controller;

import com.safar.listing.dto.RegisterHospitalRequest;
import com.safar.listing.entity.HospitalPartner;
import com.safar.listing.service.MedicalStayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/hospital-partners")
@RequiredArgsConstructor
public class AdminHospitalController {

    private final MedicalStayService medicalStayService;

    @PostMapping
    public ResponseEntity<HospitalPartner> addHospital(Authentication auth,
                                                        @Valid @RequestBody RegisterHospitalRequest req) {
        requireAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(medicalStayService.registerHospital(req));
    }

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("Admin access required");
        }
    }
}
