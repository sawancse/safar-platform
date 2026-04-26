package com.safar.services.controller;

import com.safar.services.dto.StaffMemberRequest;
import com.safar.services.entity.StaffMember;
import com.safar.services.service.StaffMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chefs/me/staff")
@RequiredArgsConstructor
public class StaffMemberController {

    private final StaffMemberService staffService;

    @GetMapping
    public ResponseEntity<List<StaffMember>> listMyStaff(Authentication auth,
                                                          @RequestParam(defaultValue = "true") boolean activeOnly,
                                                          @RequestParam(required = false) String role,
                                                          @RequestParam(defaultValue = "false") boolean includePool) {
        UUID userId = UUID.fromString(auth.getName());
        if (role != null && !role.isBlank()) {
            return ResponseEntity.ok(staffService.listStaffByRole(userId, role));
        }
        if (includePool) {
            return ResponseEntity.ok(staffService.listAssignable(userId));
        }
        return ResponseEntity.ok(staffService.listMyStaff(userId, activeOnly));
    }

    @PostMapping
    public ResponseEntity<StaffMember> create(Authentication auth, @RequestBody StaffMemberRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.create(userId, req));
    }

    @PutMapping("/{staffId}")
    public ResponseEntity<StaffMember> update(Authentication auth,
                                               @PathVariable UUID staffId,
                                               @RequestBody StaffMemberRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(staffService.update(userId, staffId, req));
    }

    @DeleteMapping("/{staffId}")
    public ResponseEntity<Void> remove(Authentication auth, @PathVariable UUID staffId) {
        UUID userId = UUID.fromString(auth.getName());
        staffService.softDelete(userId, staffId);
        return ResponseEntity.noContent().build();
    }
}
