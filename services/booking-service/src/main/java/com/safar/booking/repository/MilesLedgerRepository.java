package com.safar.booking.repository;

import com.safar.booking.entity.MilesLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MilesLedgerRepository extends JpaRepository<MilesLedger, UUID> {
    Page<MilesLedger> findByUserId(UUID userId, Pageable pageable);
}
