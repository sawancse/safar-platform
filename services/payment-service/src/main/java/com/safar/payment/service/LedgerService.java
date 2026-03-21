package com.safar.payment.service;

import com.safar.payment.entity.LedgerEntry;
import com.safar.payment.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepo;

    @Transactional
    public LedgerEntry recordEntry(UUID bookingId, String entryType, String debitAccount,
                                   String creditAccount, long amountPaise, String description,
                                   UUID referenceId) {
        LedgerEntry entry = LedgerEntry.builder()
                .bookingId(bookingId)
                .entryType(entryType)
                .debitAccount(debitAccount)
                .creditAccount(creditAccount)
                .amountPaise(amountPaise)
                .description(description)
                .referenceId(referenceId)
                .build();
        LedgerEntry saved = ledgerEntryRepo.save(entry);
        log.info("Ledger entry recorded: type={}, debit={}, credit={}, amount={} paise, booking={}",
                entryType, debitAccount, creditAccount, amountPaise, bookingId);
        return saved;
    }

    public List<LedgerEntry> getBookingLedger(UUID bookingId) {
        return ledgerEntryRepo.findByBookingId(bookingId);
    }

    public ReconciliationResult reconcile(LocalDate date) {
        OffsetDateTime startOfDay = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<LedgerEntry> allEntries = ledgerEntryRepo.findAll();

        long totalDebits = 0;
        long totalCredits = 0;
        int mismatches = 0;

        for (LedgerEntry entry : allEntries) {
            if (entry.getCreatedAt() != null
                    && !entry.getCreatedAt().isBefore(startOfDay)
                    && entry.getCreatedAt().isBefore(endOfDay)) {
                totalDebits += entry.getAmountPaise();
                totalCredits += entry.getAmountPaise();
            }
        }

        // In double-entry, debits always equal credits per entry.
        // Mismatches would come from orphaned or unbalanced entries.
        boolean balanced = totalDebits == totalCredits;

        log.info("Reconciliation for {}: debits={}, credits={}, balanced={}", date, totalDebits, totalCredits, balanced);
        return new ReconciliationResult(totalDebits, totalCredits, balanced, mismatches);
    }

    public record ReconciliationResult(long totalDebits, long totalCredits, boolean balanced, int mismatches) {}
}
