package com.safar.supply.adapter.udaan;

import com.safar.supply.adapter.OrderStatusSnapshot;
import com.safar.supply.adapter.SupplierAdapter;
import com.safar.supply.adapter.SupplierAdapterException;
import com.safar.supply.adapter.SupplierAdapterRegistry;
import com.safar.supply.entity.PurchaseOrder;
import com.safar.supply.entity.PurchaseOrderItem;
import com.safar.supply.entity.Supplier;
import com.safar.supply.entity.SupplierCatalogItem;
import com.safar.supply.entity.enums.IntegrationType;
import com.safar.supply.entity.enums.PurchaseOrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Adapter for Udaan B2B (https://udaan.com).
 *
 * Status: STUB. All four methods throw SupplierAdapterException("Not
 * implemented") at the HTTP call site. Status mapping + auth header shape
 * + endpoint paths reflect Udaan's documented partner API as of research,
 * but the actual request/response JSON is not validated until sandbox creds
 * arrive.
 *
 * To finish this adapter:
 *   1. Apply for Udaan partner API access (see docs/supplier-partnership-checklist.md §1)
 *   2. Get sandbox credentials → set udaan.base-url and udaan.api-key
 *   3. Set supply.adapters.enabled=true in application.yml
 *   4. Switch a test supplier to integrationType=UDAAN with integrationConfig
 *      = {"accountNumber":"...","warehouseCode":"..."}
 *   5. Replace each TODO(udaan-msa) below with the real RestTemplate call
 *   6. Run sandbox PO end-to-end before promoting to prod
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UdaanSupplierAdapter implements SupplierAdapter {

    private final RestTemplate restTemplate;
    @Lazy
    private final SupplierAdapterRegistry registry;   // for loading supplier config

    @Value("${udaan.base-url:https://api.udaan.com}")
    private String baseUrl;

    @Value("${udaan.api-key:}")
    private String apiKey;

    @Override
    public IntegrationType integrationType() {
        return IntegrationType.UDAAN;
    }

    @Override
    public String placePo(PurchaseOrder po, List<PurchaseOrderItem> items) {
        Supplier s = registry.loadSupplier(po.getSupplierId());
        log.info("[UdaanAdapter] placePo {} (ext lines={}) supplier={} cfg={}",
                po.getPoNumber(), items.size(), s.getBusinessName(), s.getIntegrationConfig());

        // TODO(udaan-msa): real call.
        //
        // POST {baseUrl}/v1/orders
        // Headers: Authorization: Bearer {apiKey}
        //          X-Udaan-Account: {fromIntegrationConfig.accountNumber}
        //          Content-Type: application/json
        // Body: {
        //   "buyer_account_id": cfg.accountNumber,
        //   "warehouse_code":   cfg.warehouseCode,
        //   "delivery_address": po.deliveryAddress,
        //   "external_ref":     po.poNumber,
        //   "lines": items.stream().map(i -> Map.of(
        //       "sku_id",       i.itemKey,            // Udaan SKU id (ours == theirs after catalog sync)
        //       "qty",          i.qty,
        //       "unit_price":   i.unitPricePaise / 100   // Udaan wants rupees
        //   ))
        // }
        // → return response.body.order_id;
        //
        // On 4xx: throw SupplierAdapterException(msg, retryable=false)
        // On 5xx / IO: throw SupplierAdapterException(msg, retryable=true)

        throw new SupplierAdapterException(
                "UdaanSupplierAdapter.placePo not implemented yet. "
                + "Sign Udaan partner API agreement and replace the TODO in this method.",
                false);
    }

    @Override
    public OrderStatusSnapshot fetchStatus(PurchaseOrder po) {
        // TODO(udaan-msa):
        //   GET {baseUrl}/v1/orders/{po.externalRef}
        //   parse json.status (Udaan strings) and map via mapStatus()
        //   parse json.estimated_delivery → OffsetDateTime
        //   parse json.tracking_url
        throw new SupplierAdapterException(
                "UdaanSupplierAdapter.fetchStatus not implemented yet.", true);
    }

    @Override
    public List<SupplierCatalogItem> fetchCatalog(UUID supplierId) {
        Supplier s = registry.loadSupplier(supplierId);
        log.info("[UdaanAdapter] fetchCatalog supplier={} cfg={}", s.getBusinessName(), s.getIntegrationConfig());

        // TODO(udaan-msa):
        //   GET {baseUrl}/v1/catalog?account_id={cfg.accountNumber}&page=...
        //   paginate (Udaan returns ~500 SKUs per page)
        //   normalise sku_id → snake_case item_key
        //   build SupplierCatalogItem rows (set supplierId from arg)
        throw new SupplierAdapterException(
                "UdaanSupplierAdapter.fetchCatalog not implemented yet.", true);
    }

    @Override
    public void cancelPo(PurchaseOrder po) {
        // TODO(udaan-msa):
        //   POST {baseUrl}/v1/orders/{po.externalRef}/cancel
        //   200 ok or 409 already shipped (swallow the 409)
        throw new SupplierAdapterException(
                "UdaanSupplierAdapter.cancelPo not implemented yet.", false);
    }

    /** Map Udaan status strings to our PO state machine. */
    @SuppressWarnings("unused") // used once the TODOs above are filled in
    static PurchaseOrderStatus mapStatus(String udaanStatus) {
        if (udaanStatus == null) return PurchaseOrderStatus.ISSUED;
        return switch (udaanStatus.toUpperCase()) {
            case "PLACED", "ACCEPTED"                  -> PurchaseOrderStatus.ACKNOWLEDGED;
            case "PACKED", "DISPATCHED", "IN_TRANSIT"  -> PurchaseOrderStatus.IN_TRANSIT;
            case "DELIVERED", "COMPLETED"              -> PurchaseOrderStatus.DELIVERED;
            case "CANCELLED", "REJECTED"               -> PurchaseOrderStatus.CANCELLED;
            default                                    -> PurchaseOrderStatus.ISSUED;
        };
    }

    /** Helper for callers that build the Authorization header. */
    @SuppressWarnings("unused")
    private String authHeader() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new SupplierAdapterException("udaan.api-key is not configured", false);
        }
        return "Bearer " + apiKey;
    }
}
