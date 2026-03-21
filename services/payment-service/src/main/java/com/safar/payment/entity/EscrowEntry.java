package com.safar.payment.entity;

import com.safar.payment.entity.enums.EscrowStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "escrow_entries", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscrowEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID bookingId;

    @Column(nullable = false)
    private Long amountPaise;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EscrowStatus status = EscrowStatus.HELD;

    @Column(length = 50)
    private String milestone;

    @Builder.Default
    private OffsetDateTime heldAt = OffsetDateTime.now();

    private OffsetDateTime releasedAt;

    @Column(length = 30)
    private String releasedTo;

    @Builder.Default
    private Long releasedAmountPaise = 0L;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
