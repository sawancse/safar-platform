package com.safar.services.controller;

import com.safar.services.dto.StaffMemberRequest;
import com.safar.services.entity.StaffMember;
import com.safar.services.repository.StaffMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin-managed platform staff pool (Phase B).
 * Staff rows here have chef_id = NULL and can be picked by any chef
 * when assigning staff to event bookings.
 */
@RestController
@RequestMapping("/api/v1/staff/admin")
@RequiredArgsConstructor
public class AdminStaffPoolController {

    private final StaffMemberRepository staffRepo;

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) throw new AccessDeniedException("Admin access required");
    }

    @GetMapping("/pool")
    public ResponseEntity<List<StaffMember>> listPool(Authentication auth,
                                                      @RequestParam(defaultValue = "true") boolean activeOnly,
                                                      @RequestParam(required = false) String role) {
        requireAdmin(auth);
        if (role != null && !role.isBlank()) {
            return ResponseEntity.ok(staffRepo.findByChefIdIsNullAndRoleAndActiveTrue(role));
        }
        return ResponseEntity.ok(activeOnly
                ? staffRepo.findByChefIdIsNullAndActiveTrueOrderByCreatedAtDesc()
                : staffRepo.findByChefIdIsNullOrderByCreatedAtDesc());
    }

    @PostMapping("/pool")
    public ResponseEntity<StaffMember> create(Authentication auth, @RequestBody StaffMemberRequest req) {
        requireAdmin(auth);
        StaffMember member = StaffMember.builder()
                .chefId(null)           // platform-owned
                .name(req.name())
                .role(req.role())
                .phone(req.phone())
                .photoUrl(req.photoUrl())
                .hourlyRatePaise(req.hourlyRatePaise())
                .languages(req.languages())
                .yearsExperience(req.yearsExperience())
                .notes(req.notes())
                .active(req.active() == null ? Boolean.TRUE : req.active())
                .kycStatus("VERIFIED")  // admin-added staff are considered pre-verified
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(staffRepo.save(member));
    }

    @PutMapping("/pool/{staffId}")
    public ResponseEntity<StaffMember> update(Authentication auth,
                                               @PathVariable UUID staffId,
                                               @RequestBody StaffMemberRequest req) {
        requireAdmin(auth);
        StaffMember member = staffRepo.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
        if (member.getChefId() != null) {
            throw new IllegalStateException("Staff " + staffId + " is chef-owned, not a pool member");
        }
        if (req.name() != null)             member.setName(req.name());
        if (req.role() != null)             member.setRole(req.role());
        if (req.phone() != null)            member.setPhone(req.phone());
        if (req.photoUrl() != null)         member.setPhotoUrl(req.photoUrl());
        if (req.hourlyRatePaise() != null)  member.setHourlyRatePaise(req.hourlyRatePaise());
        if (req.languages() != null)        member.setLanguages(req.languages());
        if (req.yearsExperience() != null)  member.setYearsExperience(req.yearsExperience());
        if (req.notes() != null)            member.setNotes(req.notes());
        if (req.active() != null)           member.setActive(req.active());
        return ResponseEntity.ok(staffRepo.save(member));
    }

    @DeleteMapping("/pool/{staffId}")
    public ResponseEntity<Void> softDelete(Authentication auth, @PathVariable UUID staffId) {
        requireAdmin(auth);
        StaffMember member = staffRepo.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
        if (member.getChefId() != null) {
            throw new IllegalStateException("Staff " + staffId + " is chef-owned, not a pool member");
        }
        member.setActive(false);
        staffRepo.save(member);
        return ResponseEntity.noContent().build();
    }
}
