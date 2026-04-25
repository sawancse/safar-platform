# Supplier Adapter — Design Doc

**Status:** Design only. No code added yet. Implementation triggered by first signed integration partner (per `prd-supply-chain.md` Phase 2).

**Goal:** Plug real B2B supplier APIs (Udaan, FNP, Amazon Business, …) into the existing `supply-service` without changing any caller-side code (admin UI, chef-service consume call, PO state machine all stay the same).

**Constraint:** Adding integration must be **strictly additive**. The current `MANUAL` workflow keeps working unchanged for any supplier where `integration_type = 'MANUAL'`.

---

## 1. The shape of the change

Today's PO flow:
```
Admin clicks "Issue PO" → PurchaseOrderService.transition(ISSUED)
                       → status='ISSUED', orderedAt=now
                       → (no external call — admin shares PO PDF over WhatsApp manually)
```

Phase 2 PO flow with adapter:
```
Admin clicks "Issue PO" → PurchaseOrderService.transition(ISSUED)
                       → status='ISSUED', orderedAt=now
                       → SupplierAdapterRegistry.forSupplier(supplierId)
                       → adapter.placePo(po, items)        ← REAL API CALL
                       → po.externalRef = adapter response id
                       → poRepo.save(po)
```

The state machine is unchanged. The adapter call is layered in only on transitions that actually leave Safar (ISSUED, INVOICED-fetch, etc.).

---

## 2. Schema delta (when ready)

### V2 migration sketch (do NOT run until first integration is signed)

```sql
-- V2: introduce integration metadata for suppliers + POs.
ALTER TABLE supply.suppliers
  ADD COLUMN integration_type    VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
  -- MANUAL / UDAAN / FNP / AMAZON_BUSINESS / METRO_CASH_CARRY / ...
  ADD COLUMN integration_config  JSONB,
  -- per-supplier creds path, account_id, default_warehouse_id, etc.
  ADD COLUMN catalog_synced_at   TIMESTAMPTZ;

ALTER TABLE supply.purchase_orders
  ADD COLUMN external_ref        VARCHAR(60),
  -- supplier's order id after we placed via API
  ADD COLUMN external_status     VARCHAR(30),
  -- last-known supplier status (PLACED, PACKED, OUT_FOR_DELIVERY, ...)
  ADD COLUMN external_synced_at  TIMESTAMPTZ,
  ADD COLUMN external_error      TEXT;
  -- last error from supplier API (if call failed)

CREATE INDEX idx_po_external_ref ON supply.purchase_orders (external_ref);
```

**Why config as JSONB:** every supplier wants different fields (Udaan = `accountNumber + warehouseCode`, FNP = `partnerId + senderBranch`, Amazon = `iamRoleArn + region`). JSONB absorbs the variance without schema churn.

---

## 3. Java interface

```java
package com.safar.supply.adapter;

import com.safar.supply.entity.PurchaseOrder;
import com.safar.supply.entity.PurchaseOrderItem;
import com.safar.supply.entity.SupplierCatalogItem;
import java.util.List;

/**
 * Adapter contract — every external supplier integration implements this.
 * Method calls are synchronous; long-running operations (catalog sync) run
 * via the @Scheduled job, not from the request thread.
 *
 * Implementations live in `com.safar.supply.adapter.<SUPPLIER>` packages
 * and are picked up by {@link SupplierAdapterRegistry} via Spring DI.
 */
public interface SupplierAdapter {

    /** Discriminator value matching supplier.integration_type. */
    String integrationType();

    /**
     * Push a PO to the supplier. Called when PurchaseOrderService transitions
     * to ISSUED for a supplier whose integration_type != 'MANUAL'.
     *
     * @return supplier's order id (we persist this to po.external_ref)
     * @throws SupplierAdapterException on API failure (retried by caller)
     */
    String placePo(PurchaseOrder po, List<PurchaseOrderItem> items) throws SupplierAdapterException;

    /**
     * Fetch latest order status from the supplier. Called by the
     * @Scheduled poll job for in-flight POs (ISSUED / ACK / IN_TRANSIT).
     */
    OrderStatusSnapshot fetchStatus(PurchaseOrder po) throws SupplierAdapterException;

    /**
     * Pull supplier's current catalog. Called daily for active suppliers
     * with this integration_type. Returns the supplier's full SKU list.
     * Called from a worker thread — this can be slow.
     */
    List<SupplierCatalogItem> fetchCatalog(java.util.UUID supplierId) throws SupplierAdapterException;

    /**
     * Cancel a PO that's not yet shipped. Best-effort — the supplier may
     * refuse if already packed.
     */
    void cancelPo(PurchaseOrder po) throws SupplierAdapterException;

    /** Returned by fetchStatus(). */
    record OrderStatusSnapshot(
            String externalStatus,        // supplier's raw status
            com.safar.supply.entity.enums.PurchaseOrderStatus mappedStatus,
            java.time.OffsetDateTime estimatedDelivery,
            String trackingUrl
    ) {}
}
```

```java
package com.safar.supply.adapter;

public class SupplierAdapterException extends RuntimeException {
    private final boolean retryable;

    public SupplierAdapterException(String msg, boolean retryable) {
        super(msg);
        this.retryable = retryable;
    }
    public SupplierAdapterException(String msg, Throwable cause, boolean retryable) {
        super(msg, cause);
        this.retryable = retryable;
    }
    public boolean isRetryable() { return retryable; }
}
```

---

## 4. Registry / lookup

```java
package com.safar.supply.adapter;

import com.safar.supply.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SupplierAdapterRegistry {

    private final List<SupplierAdapter> allAdapters;     // Spring auto-injects all impls
    private final SupplierRepository supplierRepo;
    private Map<String, SupplierAdapter> byType;

    @jakarta.annotation.PostConstruct
    void init() {
        byType = allAdapters.stream()
                .collect(Collectors.toMap(SupplierAdapter::integrationType, a -> a));
    }

    /** Look up the adapter for a given supplier. Returns Manual adapter for non-integrated suppliers. */
    public SupplierAdapter forSupplier(UUID supplierId) {
        var s = supplierRepo.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));
        var adapter = byType.get(s.getIntegrationType());
        if (adapter == null) {
            throw new IllegalStateException("No adapter registered for type: " + s.getIntegrationType());
        }
        return adapter;
    }
}
```

---

## 5. Three implementations on day one

### 5.1 ManualSupplierAdapter (always shipped, no-op)

```java
package com.safar.supply.adapter.manual;

import com.safar.supply.adapter.SupplierAdapter;
import com.safar.supply.adapter.SupplierAdapterException;
import com.safar.supply.entity.PurchaseOrder;
import com.safar.supply.entity.PurchaseOrderItem;
import com.safar.supply.entity.SupplierCatalogItem;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

/**
 * No-op adapter for MANUAL suppliers. Returns a synthetic ref so the rest
 * of the flow doesn't have to special-case "no integration".
 */
@Component
public class ManualSupplierAdapter implements SupplierAdapter {

    @Override public String integrationType() { return "MANUAL"; }

    @Override
    public String placePo(PurchaseOrder po, List<PurchaseOrderItem> items) {
        return "MANUAL-" + po.getPoNumber();   // synthetic ref
    }

    @Override
    public OrderStatusSnapshot fetchStatus(PurchaseOrder po) {
        // Manual POs don't poll — admin advances state by hand.
        return new OrderStatusSnapshot(po.getStatus().name(), po.getStatus(), null, null);
    }

    @Override
    public List<SupplierCatalogItem> fetchCatalog(UUID supplierId) {
        return List.of();   // catalog managed by admin in the UI
    }

    @Override
    public void cancelPo(PurchaseOrder po) {
        // No external state to cancel.
    }
}
```

### 5.2 UdaanSupplierAdapter (skeleton)

```java
package com.safar.supply.adapter.udaan;

import com.safar.supply.adapter.SupplierAdapter;
import com.safar.supply.adapter.SupplierAdapterException;
import com.safar.supply.entity.PurchaseOrder;
import com.safar.supply.entity.PurchaseOrderItem;
import com.safar.supply.entity.SupplierCatalogItem;
import com.safar.supply.entity.enums.PurchaseOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UdaanSupplierAdapter implements SupplierAdapter {

    private final RestTemplate http;            // configured with Udaan base URL + bearer token
    @Value("${udaan.base-url}") private String baseUrl;
    @Value("${udaan.api-key}")  private String apiKey;

    @Override public String integrationType() { return "UDAAN"; }

    @Override
    public String placePo(PurchaseOrder po, List<PurchaseOrderItem> items) {
        // POST {baseUrl}/v1/orders
        // body = { "buyer_account_id": ..., "delivery_address": ..., "lines": [...] }
        // header = { "Authorization": "Bearer <apiKey>" }
        // map response.order_id → return
        throw new SupplierAdapterException("Not implemented yet", false);
    }

    @Override
    public OrderStatusSnapshot fetchStatus(PurchaseOrder po) {
        // GET {baseUrl}/v1/orders/{po.externalRef}
        // map udaan_status → PurchaseOrderStatus
        throw new SupplierAdapterException("Not implemented yet", false);
    }

    @Override
    public List<SupplierCatalogItem> fetchCatalog(UUID supplierId) {
        // GET {baseUrl}/v1/catalog?account_id=...
        // map udaan SKUs → SupplierCatalogItem rows (item_key normalised to snake_case)
        throw new SupplierAdapterException("Not implemented yet", false);
    }

    @Override
    public void cancelPo(PurchaseOrder po) {
        // POST {baseUrl}/v1/orders/{externalRef}/cancel
        throw new SupplierAdapterException("Not implemented yet", false);
    }

    /** Map Udaan status strings to our state machine. */
    private PurchaseOrderStatus mapStatus(String udaan) {
        return switch (udaan) {
            case "PLACED", "ACCEPTED"             -> PurchaseOrderStatus.ACKNOWLEDGED;
            case "PACKED", "DISPATCHED", "IN_TRANSIT" -> PurchaseOrderStatus.IN_TRANSIT;
            case "DELIVERED"                      -> PurchaseOrderStatus.DELIVERED;
            case "CANCELLED"                      -> PurchaseOrderStatus.CANCELLED;
            default                               -> PurchaseOrderStatus.ISSUED;
        };
    }
}
```

### 5.3 FnpSupplierAdapter (skeleton)

Same shape as Udaan; differences:
- Path templates: `/api/b2b/v2/orders` (not `/v1/orders`)
- Auth: `X-FNP-Partner-Id` + `X-FNP-Token` headers (not bearer)
- Catalog returns floral SKUs with seasonal price; sync **daily**, not on-demand
- Cancellation almost never accepted — `cancelPo` should set `external_error="CANCEL_REJECTED_BY_FNP"` and continue locally

---

## 6. Where the registry hooks into existing code

```java
// PurchaseOrderService.transition() — TODAY:
case ISSUED -> {
    if (po.getOrderedAt() == null) po.setOrderedAt(OffsetDateTime.now());
}

// PurchaseOrderService.transition() — PHASE 2:
case ISSUED -> {
    if (po.getOrderedAt() == null) po.setOrderedAt(OffsetDateTime.now());
    SupplierAdapter adapter = adapterRegistry.forSupplier(po.getSupplierId());
    try {
        String externalRef = adapter.placePo(po, poItemRepo.findByPoIdOrderByItemLabelAsc(po.getId()));
        po.setExternalRef(externalRef);
        po.setExternalStatus("PLACED");
        po.setExternalSyncedAt(OffsetDateTime.now());
    } catch (SupplierAdapterException e) {
        // Don't fail the transition — the PO is in our system as ISSUED;
        // a retry job will re-attempt placement.
        po.setExternalError(e.getMessage());
        log.error("placePo failed for {}: {}", po.getPoNumber(), e.getMessage());
    }
}
```

**Same pattern** for `DELIVERED` (no external call needed, state already known) and `CANCELLED` (call adapter.cancelPo; tolerate failure).

---

## 7. Catalog sync job

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogSyncJob {

    private final SupplierRepository supplierRepo;
    private final SupplierCatalogItemRepository catalogRepo;
    private final SupplierAdapterRegistry registry;

    /** Daily 03:00 IST. */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Kolkata")
    public void syncAll() {
        for (Supplier s : supplierRepo.findByActiveTrueOrderByBusinessNameAsc()) {
            if ("MANUAL".equals(s.getIntegrationType())) continue;
            try {
                SupplierAdapter adapter = registry.forSupplier(s.getId());
                List<SupplierCatalogItem> fresh = adapter.fetchCatalog(s.getId());
                upsertCatalog(s.getId(), fresh);
                s.setCatalogSyncedAt(OffsetDateTime.now());
                supplierRepo.save(s);
            } catch (Exception e) {
                log.error("Catalog sync failed for {}", s.getBusinessName(), e);
            }
        }
    }

    /** Upsert by (supplier_id, item_key); soft-delete missing rows. */
    private void upsertCatalog(UUID supplierId, List<SupplierCatalogItem> fresh) { /* ... */ }
}
```

---

## 8. Status poll job

```java
@Scheduled(fixedRateString = "PT15M")     // every 15 min
public void pollInFlightPos() {
    var inFlight = poRepo.findByStatusInOrderByCreatedAtDesc(List.of(
        PurchaseOrderStatus.ISSUED, PurchaseOrderStatus.ACKNOWLEDGED, PurchaseOrderStatus.IN_TRANSIT
    ));
    for (PurchaseOrder po : inFlight) {
        SupplierAdapter adapter = registry.forSupplier(po.getSupplierId());
        if ("MANUAL".equals(adapter.integrationType())) continue;     // admin-driven
        try {
            var snap = adapter.fetchStatus(po);
            if (snap.mappedStatus() != po.getStatus()) {
                // Use existing transition() so side effects fire (e.g. stock receive on DELIVERED).
                poService.transition(po.getId(), snap.mappedStatus());
            }
            po.setExternalStatus(snap.externalStatus());
            po.setExternalSyncedAt(OffsetDateTime.now());
            poRepo.save(po);
        } catch (Exception e) {
            log.warn("Status poll failed for {}: {}", po.getPoNumber(), e.getMessage());
        }
    }
}
```

---

## 9. Failure handling

| Scenario | Behaviour |
|---|---|
| Adapter throws on placePo | PO stays ISSUED with `external_error` set; admin sees red banner + "Retry placement" button |
| Adapter API down | Exponential backoff retry job picks it up next tick; admin not paged unless 3 consecutive failures |
| Status poll returns CANCELLED but our PO isn't | Use `transition(CANCELLED)` — admin sees the change in UI |
| Catalog sync drops a SKU | Soft-delete catalog row (active=false); existing POs that reference it keep working via denormalised fields |
| Webhook arrives for unknown external_ref | 404 — log + drop. Suppliers re-send. |

---

## 10. Config per supplier (JSONB schema by adapter)

```json
// MANUAL — empty
{}

// UDAAN
{
  "accountNumber": "UDN-12345",
  "warehouseCode": "BLR-WH-01",
  "creditLineId": "CL-789",
  "webhookSecret": "rotated-quarterly-stored-in-1pw"
}

// FNP
{
  "partnerId": "SAFAR-IND",
  "senderBranchCode": "BLR-MG-01",
  "shippingModel": "WHITE_LABEL",        // or BULK_TO_WAREHOUSE
  "defaultLeadHours": 12
}

// AMAZON_BUSINESS
{
  "accountId": "amzn-business-acc-id",
  "iamRoleArn": "arn:aws:iam::...:role/AmazonBusinessApi",
  "marketplaceId": "A21TJRUUN4KGV",   // .in marketplace
  "defaultShipMethod": "STANDARD"
}
```

Credentials live outside the JSON — referenced by 1Password/Secrets Manager path. Never store API keys in DB.

---

## 11. Webhooks (incoming)

When suppliers POST status updates back to us:

```
POST /api/v1/internal/suppliers/<integration_type>/webhook
```

Each adapter has a webhook handler that:
1. Verifies signature/secret
2. Parses payload to its native shape
3. Maps to our `transition()` if status changed
4. ACKs 200 immediately (process async if heavy)

Path is `internal` so it's permitAll in supply-service security (the signature check inside the handler is the real auth). Gateway exposes only with `Path=/api/v1/internal/suppliers/**` whitelisted to specific source IPs (Udaan / FNP datacentre ranges) — TBD with each partner.

---

## 12. Test strategy

**Unit:**
- Each adapter mocked for both success + failure paths
- Status mapping exhaustive (every supplier status → our enum)
- Registry returns Manual when supplier has no integration_type set

**Integration (sandbox):**
- Sandbox supplier accounts + WireMock fallback for offline CI
- Full PO lifecycle: place → status poll x N → mark delivered → confirm stock IN movement
- Idempotency: same external_ref shouldn't double-place

**Smoke (production-shadow):**
- Run real-supplier sandbox in staging until 100 successful POs
- Roll one supplier at a time to prod (Udaan first, FNP second, Amazon last)

---

## 13. Out of scope (call-out)

- No real-time WebSocket / SSE from suppliers — webhooks are polled-back as needed
- No multi-currency — assume INR throughout
- No supplier-side return management UI for vendors — that's a Phase 3 vendor portal
- No automatic credit-limit enforcement — admin sees credit_limit_paise but PO placement doesn't block on it

---

## 14. Migration path (when to start coding)

1. Sign at least one MSA (Udaan most likely)
2. Get sandbox creds
3. Create branch `feat/supplier-adapter-v1`
4. Land V2 migration (column add) behind feature flag `supply.adapters.enabled=false`
5. Land `SupplierAdapter` interface + `ManualSupplierAdapter` only — full backward compat
6. Land `UdaanSupplierAdapter` against sandbox
7. Add the registry hook into `PurchaseOrderService.transition` behind the flag
8. Flip flag for Udaan-only test supplier in staging
9. Promote to prod after 50+ successful sandbox POs
10. Repeat for FNP, then Amazon
