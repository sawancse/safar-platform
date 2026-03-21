package com.safar.user.controller;

import com.safar.user.dto.SubmitShareRequest;
import com.safar.user.dto.WalletDto;
import com.safar.user.entity.SocialShare;
import com.safar.user.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/me")
    public ResponseEntity<WalletDto> getWallet(Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(walletService.getWallet(guestId));
    }

    @PostMapping("/shares/submit")
    public ResponseEntity<SocialShare> submitShare(Authentication auth,
                                                    @Valid @RequestBody SubmitShareRequest req) {
        UUID guestId = UUID.fromString(auth.getName());
        SocialShare share = walletService.submitShare(
                guestId, req.bookingId(), req.platform(), req.shareProofUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(share);
    }
}
