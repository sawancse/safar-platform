package com.safar.supply.adapter.fnp;

import com.safar.supply.adapter.OrderStatusSnapshot;
import com.safar.supply.adapter.SupplierAdapter;
import com.safar.supply.adapter.SupplierAdapterException;
import com.safar.supply.entity.PurchaseOrder;
import com.safar.supply.entity.PurchaseOrderItem;
import com.safar.supply.entity.Supplier;
import com.safar.supply.entity.SupplierCatalogItem;
import com.safar.supply.entity.enums.IntegrationType;
import com.safar.supply.entity.enums.PurchaseOrderStatus;
import com.safar.supply.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

/**
 * Adapter for FernsNPetals B2B (https://fnp.com).
 *
 * Status: STUB. Endpoints and auth header conventions reflect FNP's
 * partner-API documentation as of research; not validated until sandbox.
 *
 * Quirks vs Udaan (note before filling in TODOs):
 *   - Auth: X-FNP-Partner-Id + X-FNP-Token headers, NOT bearer
 *   - Floral SKUs are seasonal — catalog sync MUST run daily, not on-demand
 *   - Cancellation almost always refused (perishables) — swallow 409 and
 *     set externalError, don't propagate
 *   - White-label vs warehouse model differs per supplier; integrationConfig
 *     contains shippingModel: "WHITE_LABEL" | "BULK_TO_WAREHOUSE"
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FnpSupplierAdapter implements SupplierAdapter {

    private final RestTemplate restTemplate;
    private final SupplierRepository supplierRepo;

    @Value("${fnp.base-url:https://api.fnp.com/b2b/v2}")
    private String baseUrl;

    @Value("${fnp.partner-id:}")
    private String partnerId;

    @Value("${fnp.token:}")
    private String token;

    @Override
    public IntegrationType integrationType() {
        return IntegrationType.FNP;
    }

    @Override
    public String placePo(PurchaseOrder po, List<PurchaseOrderItem> items) {
        Supplier s = loadSupplier(po.getSupplierId());
        log.info("[FnpAdapter] placePo {} (lines={}) supplier={} cfg={}",
                po.getPoNumber(), items.size(), s.getBusinessName(), s.getIntegrationConfig());

        // TODO(fnp-msa): real call.
        //
        // POST {baseUrl}/orders
        // Headers: X-FNP-Partner-Id: {partnerId}
        //          X-FNP-Token:      {token}
        //          Content-Type:     application/json
        // Body: {
        //   "partner_ref":      po.poNumber,
        //   "shipping_model":   cfg.shippingModel,   // WHITE_LABEL | BULK_TO_WAREHOUSE
        //   "sender_branch":    cfg.senderBranchCode,
        //   "delivery_date":    po.expectedDelivery,
        //   "delivery_address": po.deliveryAddress,
        //   "items": items.stream().map(i -> Map.of(
        //       "sku":         i.itemKey,
        //       "quantity":    i.qty,
        //       "unit_price_inr": i.unitPricePaise / 100
        //   ))
        // }
        // → return response.fnp_order_id

        throw new SupplierAdapterException(
                "FnpSupplierAdapter.placePo not implemented yet. "
                + "Sign FNP B2B MSA and replace the TODO in this method.",
                false);
    }

    @Override
    public OrderStatusSnapshot fetchStatus(PurchaseOrder po) {
        // TODO(fnp-msa):
        //   GET {baseUrl}/orders/{po.externalRef}
        //   parse status, map via mapStatus()
        throw new SupplierAdapterException(
                "FnpSupplierAdapter.fetchStatus not implemented yet.", true);
    }

    @Override
    public List<SupplierCatalogItem> fetchCatalog(UUID supplierId) {
        Supplier s = loadSupplier(supplierId);
        log.info("[FnpAdapter] fetchCatalog supplier={} cfg={}", s.getBusinessName(), s.getIntegrationConfig());

        // TODO(fnp-msa):
        //   GET {baseUrl}/catalog?partner_id={partnerId}
        //   FNP returns floral SKUs with seasonal pricing — sync overwrites
        //   yesterday's prices entirely.
        throw new SupplierAdapterException(
                "FnpSupplierAdapter.fetchCatalog not implemented yet.", true);
    }

    @Override
    public void cancelPo(PurchaseOrder po) {
        // TODO(fnp-msa): POST {baseUrl}/orders/{externalRef}/cancel
        // 200 = cancelled, 409 = already in fulfillment (swallow + log)
        log.warn("[FnpAdapter] cancel attempted for {} — FNP rarely accepts. Set externalError instead.",
                po.getPoNumber());
        throw new SupplierAdapterException(
                "FnpSupplierAdapter.cancelPo not implemented yet.", false);
    }

    private Supplier loadSupplier(UUID supplierId) {
        return supplierRepo.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));
    }

    /** Map FNP status strings to our PO state machine. */
    @SuppressWarnings("unused")
    static PurchaseOrderStatus mapStatus(String fnpStatus) {
        if (fnpStatus == null) return PurchaseOrderStatus.ISSUED;
        return switch (fnpStatus.toUpperCase()) {
            case "RECEIVED", "CONFIRMED"                -> PurchaseOrderStatus.ACKNOWLEDGED;
            case "IN_PROCESSING", "OUT_FOR_DELIVERY"    -> PurchaseOrderStatus.IN_TRANSIT;
            case "DELIVERED"                            -> PurchaseOrderStatus.DELIVERED;
            case "CANCELLED", "FAILED"                  -> PurchaseOrderStatus.CANCELLED;
            default                                     -> PurchaseOrderStatus.ISSUED;
        };
    }
}
