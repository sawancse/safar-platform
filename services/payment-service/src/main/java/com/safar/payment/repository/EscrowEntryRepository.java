package com.safar.payment.repository;

import com.safar.payment.entity.EscrowEntry;
import com.safar.payment.entity.enums.EscrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EscrowEntryRepository extends JpaRepository<EscrowEntry, UUID> {
    List<EscrowEntry> findByBookingId(UUID bookingId);
    List<EscrowEntry> findByBookingIdAndStatus(UUID bookingId, EscrowStatus status);
}
