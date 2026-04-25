package com.safar.supply.service;

import com.safar.supply.dto.ConsumeStockRequest;
import com.safar.supply.dto.StockAdjustRequest;
import com.safar.supply.dto.StockItemRequest;
import com.safar.supply.entity.StockItem;
import com.safar.supply.entity.StockMovement;
import com.safar.supply.entity.enums.ItemCategory;
import com.safar.supply.entity.enums.MovementDirection;
import com.safar.supply.repository.StockItemRepository;
import com.safar.supply.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final StockItemRepository stockItemRepo;
    private final StockMovementRepository movementRepo;

    @Transactional(readOnly = true)
    public List<StockItem> list(ItemCategory category, boolean lowOnly) {
        if (lowOnly) return stockItemRepo.findLowStock();
        if (category != null) return stockItemRepo.findByCategoryAndActiveTrueOrderByItemLabelAsc(category);
        return stockItemRepo.findByActiveTrueOrderByItemLabelAsc();
    }

    @Transactional(readOnly = true)
    public StockItem getByKey(String itemKey) {
        return stockItemRepo.findByItemKey(itemKey)
                .orElseThrow(() -> new IllegalArgumentException("Stock item not found: " + itemKey));
    }

    @Transactional(readOnly = true)
    public List<StockMovement> movementsForItem(UUID stockItemId) {
        return movementRepo.findByStockItemIdOrderByCreatedAtDesc(stockItemId);
    }

    @Transactional(readOnly = true)
    public List<StockMovement> recentMovements() {
        return movementRepo.findTop100ByOrderByCreatedAtDesc();
    }

    @Transactional
    public StockItem upsertItem(StockItemRequest req) {
        Optional<StockItem> existing = stockItemRepo.findByItemKey(req.itemKey());
        StockItem s = existing.orElseGet(() -> StockItem.builder()
                .itemKey(req.itemKey())
                .itemLabel(req.itemLabel())
                .category(req.category())
                .unit(req.unit())
                .build());

        if (req.itemLabel() != null)    s.setItemLabel(req.itemLabel());
        if (req.category() != null)     s.setCategory(req.category());
        if (req.unit() != null)         s.setUnit(req.unit());
        if (req.reorderPoint() != null) s.setReorderPoint(req.reorderPoint());
        if (req.reorderQty() != null)   s.setReorderQty(req.reorderQty());
        if (req.active() != null)       s.setActive(req.active());
        return stockItemRepo.save(s);
    }

    /** Manual adjustment (positive or negative delta). Used for damage, count corrections, returns. */
    @Transactional
    public StockItem adjust(String itemKey, StockAdjustRequest req) {
        if (req.qtyDelta() == null || req.qtyDelta().signum() == 0)
            throw new IllegalArgumentException("qtyDelta required (non-zero)");
        if (req.reason() == null || req.reason().isBlank())
            throw new IllegalArgumentException("reason required");

        StockItem item = getByKey(itemKey);
        BigDecimal newQty = item.getOnHandQty().add(req.qtyDelta());
        item.setOnHandQty(newQty);
        stockItemRepo.save(item);

        movementRepo.save(StockMovement.builder()
                .stockItemId(item.getId())
                .itemKey(item.getItemKey())
                .direction(MovementDirection.ADJUSTMENT)
                .qty(req.qtyDelta().abs())
                .reason(req.reason())
                .refType("MANUAL")
                .notes(req.notes())
                .build());

        if (newQty.signum() < 0) {
            log.warn("Stock {} went negative: {}", itemKey, newQty);
        }
        return item;
    }

    /** Bulk debit triggered by a delivered service job (event/PG/maintenance). */
    @Transactional
    public List<StockMovement> consume(ConsumeStockRequest req) {
        if (req.items() == null || req.items().isEmpty())
            throw new IllegalArgumentException("items required");

        List<StockMovement> out = new ArrayList<>();
        for (ConsumeStockRequest.Item line : req.items()) {
            // If stock item doesn't exist, auto-create a placeholder so the movement is recorded.
            // Admin can fill in label/category later. Avoids dropping inventory data on the floor.
            StockItem item = stockItemRepo.findByItemKey(line.itemKey())
                    .orElseGet(() -> stockItemRepo.save(StockItem.builder()
                            .itemKey(line.itemKey())
                            .itemLabel(line.itemKey())
                            .category(ItemCategory.GROCERY)
                            .unit(com.safar.supply.entity.enums.ItemUnit.PIECE)
                            .build()));

            item.setOnHandQty(item.getOnHandQty().subtract(line.qty()));
            stockItemRepo.save(item);

            out.add(movementRepo.save(StockMovement.builder()
                    .stockItemId(item.getId())
                    .itemKey(item.getItemKey())
                    .direction(MovementDirection.OUT)
                    .qty(line.qty())
                    .reason("EVENT_CONSUMED")
                    .refType(req.refType())
                    .refId(req.refId())
                    .build()));
        }
        return out;
    }

    /** Increment stock when a PO is marked DELIVERED. Called from PurchaseOrderService. */
    @Transactional
    public void receive(String itemKey, String itemLabel, ItemCategory category,
                        com.safar.supply.entity.enums.ItemUnit unit,
                        BigDecimal qty, Long unitCostPaise,
                        UUID poId) {
        StockItem item = stockItemRepo.findByItemKey(itemKey).orElseGet(() ->
                stockItemRepo.save(StockItem.builder()
                        .itemKey(itemKey)
                        .itemLabel(itemLabel)
                        .category(category)
                        .unit(unit)
                        .build()));

        item.setOnHandQty(item.getOnHandQty().add(qty));
        item.setLastUnitCostPaise(unitCostPaise);
        item.setLastReceivedAt(OffsetDateTime.now());
        stockItemRepo.save(item);

        movementRepo.save(StockMovement.builder()
                .stockItemId(item.getId())
                .itemKey(itemKey)
                .direction(MovementDirection.IN)
                .qty(qty)
                .reason("PO_RECEIVED")
                .refType("PO")
                .refId(poId)
                .unitCostPaise(unitCostPaise)
                .build());
    }
}
