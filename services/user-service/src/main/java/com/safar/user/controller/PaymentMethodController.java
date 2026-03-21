package com.safar.user.controller;

import com.safar.user.dto.PaymentMethodDto;
import com.safar.user.dto.PaymentMethodRequest;
import com.safar.user.service.PaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public ResponseEntity<List<PaymentMethodDto>> getAll(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(paymentMethodService.getAll(userId));
    }

    @PostMapping
    public ResponseEntity<PaymentMethodDto> create(Authentication auth,
                                                    @Valid @RequestBody PaymentMethodRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentMethodService.create(userId, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentMethodDto> update(Authentication auth,
                                                    @PathVariable UUID id,
                                                    @Valid @RequestBody PaymentMethodRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(paymentMethodService.update(userId, id, req));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<Void> setDefault(Authentication auth, @PathVariable UUID id) {
        UUID userId = UUID.fromString(auth.getName());
        paymentMethodService.setDefault(userId, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable UUID id) {
        UUID userId = UUID.fromString(auth.getName());
        paymentMethodService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
