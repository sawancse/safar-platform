package com.safar.supply.entity;

import com.safar.supply.entity.enums.ItemCategory;
import com.safar.supply.entity.enums.ItemUnit;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "purchase_order_items", schema = "supply")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "po_id", nullable = false)
    private UUID poId;

    @Column(name = "catalog_item_id")
    private UUID catalogItemId;

    @Column(name = "item_key", nullable = false, length = 60)
    private String itemKey;

    @Column(name = "item_label", nullable = false, length = 120)
    private String itemLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemUnit unit;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal qty;

    @Column(name = "unit_price_paise", nullable = false)
    private Long unitPricePaise;

    @Column(name = "line_total_paise", nullable = false)
    private Long lineTotalPaise;

    @Column(name = "received_qty", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal receivedQty = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
