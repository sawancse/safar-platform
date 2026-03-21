package com.safar.user.controller;

import com.safar.user.dto.CoTravelerDto;
import com.safar.user.dto.CoTravelerRequest;
import com.safar.user.service.CoTravelerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/co-travelers")
@RequiredArgsConstructor
public class CoTravelerController {

    private final CoTravelerService coTravelerService;

    @GetMapping
    public ResponseEntity<List<CoTravelerDto>> getAll(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(coTravelerService.getAll(userId));
    }

    @PostMapping
    public ResponseEntity<CoTravelerDto> create(Authentication auth,
                                                 @Valid @RequestBody CoTravelerRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(coTravelerService.create(userId, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CoTravelerDto> update(Authentication auth,
                                                 @PathVariable UUID id,
                                                 @Valid @RequestBody CoTravelerRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(coTravelerService.update(userId, id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable UUID id) {
        UUID userId = UUID.fromString(auth.getName());
        coTravelerService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
