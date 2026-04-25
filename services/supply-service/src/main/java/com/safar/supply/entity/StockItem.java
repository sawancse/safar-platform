package com.safar.supply.entity;

import com.safar.supply.entity.enums.ItemCategory;
import com.safar.supply.entity.enums.ItemUnit;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_items", schema = "supply")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "item_key", unique = true, nullable = false, length = 60)
    private String itemKey;

    @Column(name = "item_label", nullable = false, length = 120)
    private String itemLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemUnit unit;

    @Column(name = "on_hand_qty", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal onHandQty = BigDecimal.ZERO;

    @Column(name = "reserved_qty", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal reservedQty = BigDecimal.ZERO;

    @Column(name = "reorder_point", precision = 12, scale = 2)
    private BigDecimal reorderPoint;

    @Column(name = "reorder_qty", precision = 12, scale = 2)
    private BigDecimal reorderQty;

    @Column(name = "last_unit_cost_paise")
    private Long lastUnitCostPaise;

    @Column(name = "last_received_at")
    private OffsetDateTime lastReceivedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
