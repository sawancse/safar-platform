package com.safar.user.service;

import com.safar.user.entity.GuestWallet;
import com.safar.user.entity.SocialShare;
import com.safar.user.entity.enums.ShareStatus;
import com.safar.user.repository.GuestWalletRepository;
import com.safar.user.repository.SocialShareRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock GuestWalletRepository walletRepo;
    @Mock SocialShareRepository shareRepo;
    @InjectMocks WalletService walletService;

    private final UUID GUEST_ID = UUID.randomUUID();

    private GuestWallet emptyWallet() {
        return GuestWallet.builder().guestId(GUEST_ID)
                .balancePaise(0L).lifetimeEarnedPaise(0L).build();
    }

    @Test
    void submitShare_awardsCredits() {
        UUID bookingId = UUID.randomUUID();
        UUID shareId   = UUID.randomUUID();

        GuestWallet wallet = emptyWallet();
        when(walletRepo.findByGuestId(GUEST_ID)).thenReturn(Optional.of(wallet));
        when(walletRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        SocialShare savedShare = SocialShare.builder()
                .id(shareId).guestId(GUEST_ID).bookingId(bookingId)
                .platform("INSTAGRAM").creditsPaise(29900L)
                .status(ShareStatus.PENDING).build();
        when(shareRepo.save(any())).thenReturn(savedShare);
        when(shareRepo.findById(shareId)).thenReturn(Optional.of(savedShare));

        walletService.submitShare(GUEST_ID, bookingId, "INSTAGRAM", "https://ig.com/post");

        assertThat(wallet.getBalancePaise()).isEqualTo(29900L);
        assertThat(wallet.getLifetimeEarnedPaise()).isEqualTo(29900L);
        assertThat(savedShare.getStatus()).isEqualTo(ShareStatus.VERIFIED);
    }

    @Test
    void applyWalletCredits_deductsBalance() {
        GuestWallet wallet = emptyWallet();
        wallet.setBalancePaise(50000L);
        when(walletRepo.findByGuestId(GUEST_ID)).thenReturn(Optional.of(wallet));
        when(walletRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        long newTotal = walletService.applyWalletCredits(GUEST_ID, 100000L, 30000L);

        assertThat(newTotal).isEqualTo(70000L);
        assertThat(wallet.getBalancePaise()).isEqualTo(20000L);
    }

    @Test
    void applyWalletCredits_cappedAtBalance() {
        GuestWallet wallet = emptyWallet();
        wallet.setBalancePaise(10000L);
        when(walletRepo.findByGuestId(GUEST_ID)).thenReturn(Optional.of(wallet));
        when(walletRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        long newTotal = walletService.applyWalletCredits(GUEST_ID, 100000L, 50000L);

        assertThat(newTotal).isEqualTo(90000L); // only 10k deducted
        assertThat(wallet.getBalancePaise()).isEqualTo(0L);
    }

    @Test
    void getWallet_noExisting_createsNew() {
        when(walletRepo.findByGuestId(GUEST_ID)).thenReturn(Optional.empty());
        GuestWallet newWallet = emptyWallet();
        when(walletRepo.save(any())).thenReturn(newWallet);

        var dto = walletService.getWallet(GUEST_ID);

        assertThat(dto.balancePaise()).isEqualTo(0L);
        verify(walletRepo).save(any());
    }
}
