package com.safar.user.service;

import com.safar.user.dto.WalletDto;
import com.safar.user.entity.GuestWallet;
import com.safar.user.entity.SocialShare;
import com.safar.user.entity.enums.ShareStatus;
import com.safar.user.repository.GuestWalletRepository;
import com.safar.user.repository.SocialShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final GuestWalletRepository walletRepo;
    private final SocialShareRepository shareRepo;

    public WalletDto getWallet(UUID guestId) {
        GuestWallet w = getOrCreate(guestId);
        return new WalletDto(w.getGuestId(), w.getBalancePaise(), w.getLifetimeEarnedPaise());
    }

    @Transactional
    public SocialShare submitShare(UUID guestId, UUID bookingId, String platform, String proofUrl) {
        SocialShare share = SocialShare.builder()
                .guestId(guestId)
                .bookingId(bookingId)
                .platform(platform)
                .shareProofUrl(proofUrl)
                .creditsPaise(29900L)
                .status(ShareStatus.PENDING)
                .build();
        SocialShare saved = shareRepo.save(share);
        awardCredits(guestId, saved.getId(), 29900L);
        return shareRepo.findById(saved.getId()).orElse(saved);
    }

    @Transactional
    public void awardCredits(UUID guestId, UUID shareId, long creditsPaise) {
        GuestWallet wallet = getOrCreate(guestId);
        wallet.setBalancePaise(wallet.getBalancePaise() + creditsPaise);
        wallet.setLifetimeEarnedPaise(wallet.getLifetimeEarnedPaise() + creditsPaise);
        walletRepo.save(wallet);
        shareRepo.findById(shareId).ifPresent(s -> {
            s.setStatus(ShareStatus.VERIFIED);
            s.setVerifiedAt(OffsetDateTime.now());
            shareRepo.save(s);
        });
    }

    /**
     * Deducts creditsToApply from the guest's wallet (capped at booking total)
     * and returns the new booking total after deduction.
     */
    @Transactional
    public long applyWalletCredits(UUID guestId, long bookingTotalPaise, long creditsToApply) {
        GuestWallet wallet = getOrCreate(guestId);
        long maxApplicable = Math.min(wallet.getBalancePaise(), bookingTotalPaise);
        long applied = Math.min(creditsToApply, maxApplicable);
        wallet.setBalancePaise(wallet.getBalancePaise() - applied);
        walletRepo.save(wallet);
        return bookingTotalPaise - applied;
    }

    public GuestWallet getOrCreate(UUID guestId) {
        return walletRepo.findByGuestId(guestId).orElseGet(() ->
                walletRepo.save(GuestWallet.builder().guestId(guestId).build()));
    }
}
