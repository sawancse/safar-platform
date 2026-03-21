package com.safar.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_methods", schema = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** UPI, CREDIT_CARD, DEBIT_CARD, NET_BANKING */
    @Column(nullable = false, length = 20)
    private String type;

    @Column(length = 100)
    private String label;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    // ── UPI ──
    @Column(name = "upi_id", length = 80)
    private String upiId;

    // ── Card (last4 only — never store full number) ──
    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    @Column(name = "card_network", length = 20)
    private String cardNetwork;

    @Column(name = "card_holder", length = 100)
    private String cardHolder;

    @Column(name = "card_expiry", length = 7)
    private String cardExpiry;

    // ── Net Banking ──
    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_last4", length = 4)
    private String bankAccountLast4;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
