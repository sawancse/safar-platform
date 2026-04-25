package com.safar.supply.entity;

import com.safar.supply.entity.enums.PurchaseOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "purchase_orders", schema = "supply")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "po_number", unique = true, nullable = false, length = 20)
    private String poNumber;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Column(name = "ordered_at")
    private OffsetDateTime orderedAt;

    @Column(name = "expected_delivery")
    private LocalDate expectedDelivery;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "invoice_number", length = 60)
    private String invoiceNumber;

    @Column(name = "invoice_paise")
    private Long invoicePaise;

    @Column(name = "invoiced_at")
    private OffsetDateTime invoicedAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "payment_ref", length = 60)
    private String paymentRef;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "total_paise", nullable = false)
    @Builder.Default
    private Long totalPaise = 0L;

    @Column(name = "tax_paise")
    @Builder.Default
    private Long taxPaise = 0L;

    @Column(name = "grand_total_paise")
    @Builder.Default
    private Long grandTotalPaise = 0L;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    // ── Integration (Phase 2) ──────────────────────────────────────────
    @Column(name = "external_ref", length = 60)
    private String externalRef;

    @Column(name = "external_status", length = 40)
    private String externalStatus;

    @Column(name = "external_synced_at")
    private OffsetDateTime externalSyncedAt;

    @Column(name = "external_error", columnDefinition = "TEXT")
    private String externalError;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
