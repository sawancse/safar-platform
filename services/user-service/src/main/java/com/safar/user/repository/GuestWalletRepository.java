package com.safar.user.repository;

import com.safar.user.entity.GuestWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GuestWalletRepository extends JpaRepository<GuestWallet, UUID> {
    Optional<GuestWallet> findByGuestId(UUID guestId);
}
