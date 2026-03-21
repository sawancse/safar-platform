package com.safar.booking.repository;

import com.safar.booking.entity.MilesBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MilesBalanceRepository extends JpaRepository<MilesBalance, UUID> {
    Optional<MilesBalance> findByUserId(UUID userId);
}
