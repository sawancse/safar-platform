package com.safar.supply.service;

import com.safar.supply.adapter.SupplierAdapter;
import com.safar.supply.adapter.SupplierAdapterException;
import com.safar.supply.adapter.SupplierAdapterRegistry;
import com.safar.supply.dto.CreatePurchaseOrderRequest;
import com.safar.supply.dto.InvoicePoRequest;
import com.safar.supply.dto.PayPoRequest;
import com.safar.supply.entity.PurchaseOrder;
import com.safar.supply.entity.PurchaseOrderItem;
import com.safar.supply.entity.Supplier;
import com.safar.supply.entity.SupplierCatalogItem;
import com.safar.supply.entity.enums.IntegrationType;
import com.safar.supply.entity.enums.PurchaseOrderStatus;
import com.safar.supply.repository.PurchaseOrderItemRepository;
import com.safar.supply.repository.PurchaseOrderRepository;
import com.safar.supply.repository.SupplierCatalogItemRepository;
import com.safar.supply.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepo;
    private final PurchaseOrderItemRepository poItemRepo;
    private final SupplierRepository supplierRepo;
    private final SupplierCatalogItemRepository catalogRepo;
    private final SupplierService supplierService;
    private final StockService stockService;
    private final SupplierAdapterRegistry adapterRegistry;

    @Value("${supply.adapters.enabled:false}")
    private boolean adaptersEnabled;

    private static final DateTimeFormatter PO_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    @Transactional(readOnly = true)
    public List<PurchaseOrder> list(PurchaseOrderStatus status, UUID supplierId) {
        if (status != null)     return poRepo.findByStatusOrderByCreatedAtDesc(status);
        if (supplierId != null) return poRepo.findBySupplierIdOrderByCreatedAtDesc(supplierId);
        return poRepo.findByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public PurchaseOrder get(UUID id) {
        return poRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PO not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderItem> listItems(UUID poId) {
        return poItemRepo.findByPoIdOrderByItemLabelAsc(poId);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> overdue() {
        return poRepo.findOverdue(LocalDate.now());
    }

    @Transactional
    public PurchaseOrder create(CreatePurchaseOrderRequest req, UUID userId) {
        if (req.supplierId() == null) throw new IllegalArgumentException("supplierId required");
        if (req.items() == null || req.items().isEmpty())
            throw new IllegalArgumentException("at least one line item required");
        supplierRepo.findById(req.supplierId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        long totalPaise = 0L;
        List<PurchaseOrderItem> stagedItems = new ArrayList<>();
        for (CreatePurchaseOrderRequest.LineItem line : req.items()) {
            // If catalog item id is set, hydrate label/category/unit/price from catalog.
            String label = line.itemLabel();
            var category = line.category();
            var unit = line.unit();
            Long unitPrice = line.unitPricePaise();
            UUID catalogItemId = null;

            if (line.catalogItemId() != null) {
                SupplierCatalogItem c = catalogRepo.findById(line.catalogItemId())
                        .orElseThrow(() -> new IllegalArgumentException("Catalog item not found"));
                if (!req.supplierId().equals(c.getSupplierId()))
                    throw new IllegalArgumentException("Catalog item does not belong to supplier");
                catalogItemId = c.getId();
                if (label == null)     label = c.getItemLabel();
                if (category == null)  category = c.getCategory();
                if (unit == null)      unit = c.getUnit();
                if (unitPrice == null) unitPrice = c.getPricePaise();
            }
            if (line.qty() == null || line.qty().signum() <= 0)
                throw new IllegalArgumentException("qty must be > 0 for " + line.itemKey());
            if (unitPrice == null)
                throw new IllegalArgumentException("unitPricePaise required for " + line.itemKey());

            long lineTotal = unitPrice * line.qty().setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact() / 100L;
            totalPaise += lineTotal;

            stagedItems.add(PurchaseOrderItem.builder()
                    .catalogItemId(catalogItemId)
                    .itemKey(line.itemKey())
                    .itemLabel(label)
                    .category(category)
                    .unit(unit)
                    .qty(line.qty())
                    .unitPricePaise(unitPrice)
                    .lineTotalPaise(lineTotal)
                    .notes(line.notes())
                    .build());
        }

        long taxPaise = req.taxPaise() != null ? req.taxPaise() : Math.round(totalPaise * 0.18);

        boolean issueNow = Boolean.TRUE.equals(req.issueImmediately());

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(generatePoNumber())
                .supplierId(req.supplierId())
                .status(issueNow ? PurchaseOrderStatus.ISSUED : PurchaseOrderStatus.DRAFT)
                .orderedAt(issueNow ? OffsetDateTime.now() : null)
                .expectedDelivery(req.expectedDelivery())
                .totalPaise(totalPaise)
                .taxPaise(taxPaise)
                .grandTotalPaise(totalPaise + taxPaise)
                .deliveryAddress(req.deliveryAddress())
                .createdByUserId(userId)
                .adminNotes(req.adminNotes())
                .build();
        po = poRepo.save(po);

        for (PurchaseOrderItem item : stagedItems) {
            item.setPoId(po.getId());
            poItemRepo.save(item);
        }
        return po;
    }

    @Transactional
    public PurchaseOrder transition(UUID id, PurchaseOrderStatus to) {
        PurchaseOrder po = get(id);
        validateTransition(po.getStatus(), to);
        po.setStatus(to);
        OffsetDateTime now = OffsetDateTime.now();
        switch (to) {
            case ISSUED -> {
                if (po.getOrderedAt() == null) po.setOrderedAt(now);
                dispatchToSupplier(po);
            }
            case DELIVERED -> {
                po.setDeliveredAt(now);
                receiveStockForPo(po);
                supplierService.incrementPosCompleted(po.getSupplierId());
            }
            default -> {}
        }
        return poRepo.save(po);
    }

    /**
     * Phase 2 adapter dispatch. Gated on supply.adapters.enabled. Fail-soft —
     * if the adapter call throws, the PO stays ISSUED in our system with
     * external_error captured; admin can retry from the UI.
     */
    private void dispatchToSupplier(PurchaseOrder po) {
        if (!adaptersEnabled) return;

        Supplier supplier = supplierRepo.findById(po.getSupplierId()).orElse(null);
        if (supplier == null || supplier.getIntegrationType() == IntegrationType.MANUAL) return;

        try {
            SupplierAdapter adapter = adapterRegistry.forType(supplier.getIntegrationType());
            List<PurchaseOrderItem> items = poItemRepo.findByPoIdOrderByItemLabelAsc(po.getId());
            String externalRef = adapter.placePo(po, items);
            po.setExternalRef(externalRef);
            po.setExternalStatus("PLACED");
            po.setExternalSyncedAt(OffsetDateTime.now());
            po.setExternalError(null);
            log.info("PO {} dispatched to {} (ref {})", po.getPoNumber(),
                    supplier.getIntegrationType(), externalRef);
        } catch (SupplierAdapterException e) {
            // Don't fail the transition — the PO is in our system as ISSUED.
            // Admin sees external_error in UI and can retry.
            po.setExternalError(e.getMessage());
            log.error("dispatchToSupplier failed for {}: {}", po.getPoNumber(), e.getMessage());
        }
    }

    @Transactional
    public PurchaseOrder invoice(UUID id, InvoicePoRequest req) {
        PurchaseOrder po = get(id);
        validateTransition(po.getStatus(), PurchaseOrderStatus.INVOICED);
        po.setStatus(PurchaseOrderStatus.INVOICED);
        po.setInvoicedAt(OffsetDateTime.now());
        po.setInvoiceNumber(req.invoiceNumber());
        po.setInvoicePaise(req.invoicePaise());
        return poRepo.save(po);
    }

    @Transactional
    public PurchaseOrder pay(UUID id, PayPoRequest req) {
        if (req.paymentRef() == null || req.paymentRef().isBlank())
            throw new IllegalArgumentException("paymentRef required");
        PurchaseOrder po = get(id);
        validateTransition(po.getStatus(), PurchaseOrderStatus.PAID);
        po.setStatus(PurchaseOrderStatus.PAID);
        po.setPaidAt(OffsetDateTime.now());
        po.setPaymentRef(req.paymentRef());
        return poRepo.save(po);
    }

    @Transactional
    public PurchaseOrder cancel(UUID id, String reason) {
        PurchaseOrder po = get(id);
        if (po.getStatus() == PurchaseOrderStatus.PAID
                || po.getStatus() == PurchaseOrderStatus.CANCELLED)
            throw new IllegalStateException("Cannot cancel PO in status " + po.getStatus());

        // Best-effort cancel at the supplier side first (only if PO was actually dispatched).
        if (adaptersEnabled && po.getExternalRef() != null) {
            Supplier supplier = supplierRepo.findById(po.getSupplierId()).orElse(null);
            if (supplier != null && supplier.getIntegrationType() != IntegrationType.MANUAL) {
                try {
                    adapterRegistry.forType(supplier.getIntegrationType()).cancelPo(po);
                } catch (SupplierAdapterException e) {
                    // Common — supplier rejects cancel after pack/ship. Log and proceed locally.
                    log.warn("Supplier-side cancel failed for {}: {}", po.getPoNumber(), e.getMessage());
                    po.setExternalError("Supplier refused cancel: " + e.getMessage());
                }
            }
        }

        po.setStatus(PurchaseOrderStatus.CANCELLED);
        po.setCancelledAt(OffsetDateTime.now());
        po.setCancelReason(reason);
        return poRepo.save(po);
    }

    private void validateTransition(PurchaseOrderStatus from, PurchaseOrderStatus to) {
        boolean ok = switch (from) {
            case DRAFT        -> to == PurchaseOrderStatus.ISSUED || to == PurchaseOrderStatus.CANCELLED;
            case ISSUED       -> to == PurchaseOrderStatus.ACKNOWLEDGED || to == PurchaseOrderStatus.IN_TRANSIT
                              || to == PurchaseOrderStatus.DELIVERED   || to == PurchaseOrderStatus.CANCELLED;
            case ACKNOWLEDGED -> to == PurchaseOrderStatus.IN_TRANSIT || to == PurchaseOrderStatus.DELIVERED
                              || to == PurchaseOrderStatus.CANCELLED;
            case IN_TRANSIT   -> to == PurchaseOrderStatus.DELIVERED || to == PurchaseOrderStatus.CANCELLED;
            case DELIVERED    -> to == PurchaseOrderStatus.INVOICED;
            case INVOICED     -> to == PurchaseOrderStatus.PAID;
            default -> false;
        };
        if (!ok) throw new IllegalStateException("Invalid transition " + from + " → " + to);
    }

    private void receiveStockForPo(PurchaseOrder po) {
        List<PurchaseOrderItem> items = poItemRepo.findByPoIdOrderByItemLabelAsc(po.getId());
        for (PurchaseOrderItem item : items) {
            BigDecimal toReceive = item.getQty().subtract(
                    item.getReceivedQty() == null ? BigDecimal.ZERO : item.getReceivedQty());
            if (toReceive.signum() <= 0) continue;
            stockService.receive(item.getItemKey(), item.getItemLabel(), item.getCategory(), item.getUnit(),
                    toReceive, item.getUnitPricePaise(), po.getId());
            item.setReceivedQty(item.getQty());
            poItemRepo.save(item);
        }
    }

    /** PO-YYYYMM-NNNN. Random 4-digit suffix; uniqueness enforced by DB constraint. */
    private String generatePoNumber() {
        return "PO-" + LocalDate.now().format(PO_MONTH) + "-" +
                String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
