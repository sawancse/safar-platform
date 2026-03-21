package com.safar.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID bookingId;

    @Column(nullable = false, length = 30)
    private String entryType;

    @Column(nullable = false, length = 50)
    private String debitAccount;

    @Column(nullable = false, length = 50)
    private String creditAccount;

    @Column(nullable = false)
    private Long amountPaise;

    @Column(length = 500)
    private String description;

    private UUID referenceId;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
