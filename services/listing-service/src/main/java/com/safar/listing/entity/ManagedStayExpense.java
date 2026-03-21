package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "managed_stay_expenses", schema = "listings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ManagedStayExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "expense_type", nullable = false, length = 50)
    private String expenseType;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(length = 255)
    private String description;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @CreationTimestamp
    @Column(name = "incurred_at", updatable = false)
    private OffsetDateTime incurredAt;
}
