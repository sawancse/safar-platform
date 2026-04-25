package com.safar.supply.entity;

import com.safar.supply.entity.enums.MovementDirection;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_movements", schema = "supply")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "stock_item_id", nullable = false)
    private UUID stockItemId;

    @Column(name = "item_key", nullable = false, length = 60)
    private String itemKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MovementDirection direction;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal qty;

    @Column(nullable = false, length = 40)
    private String reason;

    @Column(name = "ref_type", length = 30)
    private String refType;

    @Column(name = "ref_id")
    private UUID refId;

    @Column(name = "unit_cost_paise")
    private Long unitCostPaise;

    @Column(name = "performed_by_user_id")
    private UUID performedByUserId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
