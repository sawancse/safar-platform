package com.safar.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "host_expenses", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID hostId;

    private UUID listingId;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false)
    private Long amountPaise;

    @Builder.Default
    private Long gstPaise = 0L;

    @Column(length = 255)
    private String description;

    @Column(length = 500)
    private String receiptUrl;

    @Column(nullable = false)
    private LocalDate expenseDate;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
