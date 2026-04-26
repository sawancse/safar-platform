# supply-service

Supply Chain Management for Safar Cooks. Owns suppliers, catalog, purchase
orders (PO state machine: `DRAFT -> ISSUED -> ACKNOWLEDGED -> IN_TRANSIT ->
DELIVERED -> PAID`, plus `CANCELLED`), stock balances, and stock movements.

| | |
|---|---|
| Port | `8096` |
| DB schema | `supply` (Postgres, Flyway-managed) |
| Java | 17, Spring Boot 3.4.0 |
| Routes | `/api/v1/suppliers/admin`, `/api/v1/purchase-orders/admin`, `/api/v1/stock/admin`, `/api/v1/internal/stock` |

## Run

```bash
mvn -pl services/supply-service spring-boot:run
```

Tests use H2 (no infra needed):

```bash
mvn -pl services/supply-service test
```

## Migrations

- `V1__create_supply_schema.sql` -- suppliers, catalog, POs + items, stock,
  stock movements.
- `V2__supplier_integration.sql` -- adds `integration_type`,
  `integration_config` (jsonb), `catalog_synced_at` to suppliers; adds
  `external_ref`, `external_status`, `external_synced_at`, `external_error`
  to purchase orders. Strictly additive; every existing supplier defaults to
  `MANUAL`.

## Supplier Adapter SPI

External integrations live behind a small SPI in
`com.safar.supply.adapter`:

```java
public interface SupplierAdapter {
    IntegrationType integrationType();
    String placePo(PurchaseOrder po, List<PurchaseOrderItem> items);
    OrderStatusSnapshot fetchStatus(PurchaseOrder po);
    List<SupplierCatalogItem> fetchCatalog(UUID supplierId);
    void cancelPo(PurchaseOrder po);
}
```

`SupplierAdapterRegistry` collects every `SupplierAdapter` bean at startup and
indexes them by `IntegrationType`. `PurchaseOrderService.transition(ISSUED)`
calls `dispatchToSupplier(po)` which looks up the right adapter for that
supplier and forwards the call. Adapter exceptions are caught fail-soft -- the
PO stays `ISSUED` with `external_error` captured so the admin can retry from
the UI; the local transition itself never fails because of an adapter error.

`IntegrationType` values: `MANUAL`, `UDAAN`, `FNP`, `AMAZON_BUSINESS`,
`METRO_CASH_CARRY`, `JUMBOTAIL`, `NINJACART`.

| Adapter | Status | Notes |
|---|---|---|
| `ManualSupplierAdapter` | Live | No-op. Returns synthetic `MANUAL-{poNumber}` external ref so `external_ref` is never null. Catalog/cancel are no-ops. This is what runs today for every existing supplier. |
| `UdaanSupplierAdapter` | Stub | Wired (status mapping, auth header builder). HTTP calls throw `SupplierAdapterException("not implemented yet")`. Each method has a `// TODO(udaan-msa)` block describing the endpoint + body shape. |
| `FnpSupplierAdapter` | Stub | Same pattern. Quirks documented inline: `X-FNP-Partner-Id` / `X-FNP-Token` headers (not bearer), daily-only catalog sync, cancel often refused for perishables. |

## Scheduled jobs

Both gated on `supply.adapters.enabled`.

- `CatalogSyncJob` -- `@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Kolkata")`.
  Skips MANUAL suppliers. Upserts catalog rows by `(supplier_id, item_key)`,
  soft-deletes rows that vanished from the supplier feed.
- `PoStatusPollJob` -- `@Scheduled(fixedRateString = "PT15M")`. Polls in-flight
  POs (`ISSUED` / `ACKNOWLEDGED` / `IN_TRANSIT`) at integrated suppliers;
  updates `external_status` and transitions the local PO if the mapped status
  changed. Catches `IllegalStateException` for invalid transitions (e.g.
  supplier reports `IN_TRANSIT` after we've already marked `DELIVERED`).

## Configuration

```yaml
supply:
  adapters:
    enabled: ${SUPPLY_ADAPTERS_ENABLED:false}    # MASTER SWITCH

udaan:
  base-url: ${UDAAN_BASE_URL:https://api.udaan.com}
  api-key:  ${UDAAN_API_KEY:}

fnp:
  base-url:   ${FNP_BASE_URL:https://api.fnp.com/b2b/v2}
  partner-id: ${FNP_PARTNER_ID:}
  token:      ${FNP_TOKEN:}
```

Default behavior is unchanged for everyone: existing MANUAL suppliers keep
working, and `supply.adapters.enabled=false` means even a supplier flipped to
`UDAAN` results in a no-op dispatch.

## Going live with Udaan or FNP

1. Sign the partner API agreement, obtain sandbox credentials.
2. Set the env vars above (`UDAAN_API_KEY` etc.).
3. Replace the four `// TODO(udaan-msa)` (or `// TODO(fnp-msa)`) blocks in the
   adapter with real `RestTemplate` calls (~50 lines per method).
4. In admin (`/suppliers`), change the supplier's `integrationType` to `UDAAN`
   (or `FNP`) and set its `integrationConfig` JSON.
5. Set `SUPPLY_ADAPTERS_ENABLED=true` on the chef-service deployment.
6. Run 50+ POs through sandbox end-to-end before promoting to prod.

## See also

- `safar-platform/docs/prd-supply-chain.md` -- product/architecture spec.
- `safar-platform/docs/scm-architecture-diagrams.md` -- mermaid diagrams
  (end-to-end sequence, domain ER, adapter flowchart, PO state machine).
- Admin UI: `/suppliers`, `/purchase-orders`, `/stock` in the React admin app.
