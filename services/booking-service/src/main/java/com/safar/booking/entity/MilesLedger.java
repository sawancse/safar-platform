package com.safar.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "miles_ledger", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilesLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private UUID bookingId;

    @Column(nullable = false, length = 30)
    private String transactionType;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long balanceAfter;

    @Column(length = 255)
    private String description;

    private OffsetDateTime expiresAt;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
