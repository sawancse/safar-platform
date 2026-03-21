package com.safar.payment.service;

import com.safar.payment.entity.EscrowEntry;
import com.safar.payment.entity.enums.EscrowStatus;
import com.safar.payment.repository.EscrowEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscrowService {

    private final EscrowEntryRepository escrowEntryRepo;
    private final LedgerService ledgerService;

    @Transactional
    public EscrowEntry holdFunds(UUID bookingId, long amountPaise, String milestone) {
        EscrowEntry entry = EscrowEntry.builder()
                .bookingId(bookingId)
                .amountPaise(amountPaise)
                .status(EscrowStatus.HELD)
                .milestone(milestone)
                .heldAt(OffsetDateTime.now())
                .build();
        entry = escrowEntryRepo.save(entry);

        ledgerService.recordEntry(
                bookingId,
                "ESCROW_HOLD",
                "guest_receivable",
                "escrow_account",
                amountPaise,
                "Funds held in escrow for milestone: " + milestone,
                entry.getId()
        );

        log.info("Escrow hold created for booking {}: {} paise, milestone={}", bookingId, amountPaise, milestone);
        return entry;
    }

    @Transactional
    public EscrowEntry releaseFunds(UUID bookingId, String recipientType, long amountPaise, String milestone) {
        List<EscrowEntry> heldEntries = escrowEntryRepo.findByBookingIdAndStatus(bookingId, EscrowStatus.HELD);
        if (heldEntries.isEmpty()) {
            throw new IllegalStateException("No held escrow entries found for booking: " + bookingId);
        }

        EscrowEntry entry = heldEntries.get(0);
        long newReleasedAmount = entry.getReleasedAmountPaise() + amountPaise;

        if (newReleasedAmount >= entry.getAmountPaise()) {
            entry.setStatus(EscrowStatus.RELEASED);
        } else {
            entry.setStatus(EscrowStatus.PARTIAL_RELEASED);
        }

        entry.setReleasedAmountPaise(newReleasedAmount);
        entry.setReleasedAt(OffsetDateTime.now());
        entry.setReleasedTo(recipientType);
        entry.setNotes("Released for milestone: " + milestone);
        entry = escrowEntryRepo.save(entry);

        String creditAccount = recipientType.toLowerCase() + "_payable";
        ledgerService.recordEntry(
                bookingId,
                "ESCROW_RELEASE",
                "escrow_account",
                creditAccount,
                amountPaise,
                "Escrow released to " + recipientType + " for milestone: " + milestone,
                entry.getId()
        );

        log.info("Escrow released for booking {}: {} paise to {}", bookingId, amountPaise, recipientType);
        return entry;
    }

    @Transactional
    public EscrowEntry disputeHold(UUID bookingId) {
        List<EscrowEntry> heldEntries = escrowEntryRepo.findByBookingIdAndStatus(bookingId, EscrowStatus.HELD);
        if (heldEntries.isEmpty()) {
            throw new IllegalStateException("No held escrow entries found for booking: " + bookingId);
        }

        EscrowEntry entry = heldEntries.get(0);
        entry.setStatus(EscrowStatus.DISPUTE_HOLD);
        entry.setNotes("Dispute hold applied");
        entry = escrowEntryRepo.save(entry);

        log.info("Escrow dispute hold applied for booking {}", bookingId);
        return entry;
    }

    public List<EscrowEntry> getEscrowStatus(UUID bookingId) {
        return escrowEntryRepo.findByBookingId(bookingId);
    }

    /**
     * @deprecated Use {@link #getEscrowStatus(UUID)} instead.
     */
    @Deprecated
    public List<EscrowEntry> getEscrowEntries(UUID bookingId) {
        return getEscrowStatus(bookingId);
    }
}
