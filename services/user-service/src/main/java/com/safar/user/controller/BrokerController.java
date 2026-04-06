package com.safar.user.controller;

import com.safar.user.dto.BrokerProfileResponse;
import com.safar.user.dto.CreateBrokerProfileRequest;
import com.safar.user.service.BrokerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/brokers")
@RequiredArgsConstructor
public class BrokerController {

    private final BrokerProfileService brokerService;

    @PostMapping("/register")
    public ResponseEntity<BrokerProfileResponse> register(
            Authentication auth,
            @RequestBody CreateBrokerProfileRequest request) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(brokerService.createProfile(request, userId));
    }

    @GetMapping("/me")
    public ResponseEntity<BrokerProfileResponse> getMyProfile(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(brokerService.getProfile(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<BrokerProfileResponse> updateMyProfile(
            Authentication auth,
            @RequestBody CreateBrokerProfileRequest request) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(brokerService.updateProfile(request, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BrokerProfileResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(brokerService.getProfileById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<BrokerProfileResponse>> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String specialization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                brokerService.searchBrokers(city, specialization,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/admin/list")
    public ResponseEntity<Page<BrokerProfileResponse>> adminList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                brokerService.adminList(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @PutMapping("/admin/{id}/verify")
    public ResponseEntity<BrokerProfileResponse> adminVerify(@PathVariable UUID id) {
        return ResponseEntity.ok(brokerService.adminVerify(id));
    }
}
