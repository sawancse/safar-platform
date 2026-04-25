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
@Table(name = "supplier_catalog_items", schema = "supply")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierCatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

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

    @Column(name = "price_paise", nullable = false)
    private Long pricePaise;

    @Column(name = "moq_qty", precision = 12, scale = 2)
    private BigDecimal moqQty;

    @Column(name = "pack_size", precision = 12, scale = 2)
    private BigDecimal packSize;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
