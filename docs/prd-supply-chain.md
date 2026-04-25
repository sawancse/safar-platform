# PRD: Safar Supply Chain Management (SCM)

**Version:** 1.0
**Date:** 2026-04-25
**Status:** Design + MVP build in progress
**Owner:** Platform / Operations
**Reference:** Mirror of PartnerVendor pattern (admin-onboarded suppliers) + classic procurement (PO + stock + movements)

---

## 1. Executive Summary

Safar's marketplace ships services that are made of **physical things**: a cake order needs flour and fondant, a decor job needs flowers and balloons, a PG room needs linens and toiletries, a maintenance ticket needs paint and pipes. Today none of that is tracked — the platform is "demand-aware" but "supply-blind".

Supply Chain Management (SCM) closes that loop. Admin onboards **suppliers** (FreshKart, Bakery Supply Co, Decor Mart, etc.), maintains a **catalog** of buyable items per supplier, raises **Purchase Orders** when stock dips below reorder threshold, tracks **stock levels** per category, and records **stock movements** as items flow in (PO receipt) and out (consumed by a vendor service job, a PG check-in, or a maintenance ticket).

Phase 1 (MVP — this build) covers: supplier directory, supplier catalog, PO lifecycle, stock + movements, low-stock alerts. Admin-managed end-to-end. No supplier portal, no auto-reorder, no logistics integration.

**Why now:** the bespoke services marketplace (cake / decor / pandit / live-music / appliances / staff-hire) is shipping orders to vendors who silently absorb supply costs. Once we know unit cost of flour, candles, flowers, etc., we can quote bespoke orders accurately, identify low-margin SKUs, and build the next layer (auto-reorder, supplier ratings, vendor-supplier matching).

---

## 2. Goals & Non-Goals

### 2.1 Goals (MVP)
- Admin can onboard a supplier in <2 minutes (mirror PartnerVendor flow)
- Admin can view current stock per category at a glance
- Admin can raise a PO from a supplier's catalog in 1 minute
- PO lifecycle is explicit and auditable: DRAFT → ISSUED → ACK → IN_TRANSIT → DELIVERED → INVOICED → PAID
- Stock auto-increments when a PO is marked DELIVERED
- Stock auto-decrements when a vendor service job is marked DELIVERED (Phase 1 wires cake + decor only)
- Low-stock alert (Kafka event + admin badge) when item drops below reorder point

### 2.2 Non-Goals (explicit deferrals)
- Supplier self-service portal (suppliers don't log in; they get email/WhatsApp PO copy)
- Auto-reorder (alert only, admin issues PO manually)
- Multi-warehouse / multi-location stock (single virtual warehouse for v1)
- Logistics / courier integration (no shipment tracking — only "delivered yes/no")
- Demand forecasting / ML (stick to threshold-based reorder)
- Cost-of-goods-sold accounting & vendor markup analysis (separate phase)
- Supplier marketplace bidding / quotes (admin picks supplier directly)

---

## 3. User Stories

### 3.1 Operations Admin
- **As ops admin**, I want to see what we have in stock right now so I can answer "do we have enough flour for tomorrow's cake orders?"
- **As ops admin**, I want to receive an alert when fondant drops below 20 kg so I can reorder before we stockout
- **As ops admin**, I want to raise a PO with FreshKart for groceries in one click rather than copy-pasting prices into WhatsApp
- **As ops admin**, I want to see all open POs with overdue deliveries so I can chase suppliers

### 3.2 Finance Admin
- **As finance**, I want to see total open POs by supplier so I can plan cash outflow
- **As finance**, I want to mark a PO PAID when I've completed the NEFT, with the UTR captured

### 3.3 Vendor Coordinator (uses CooksPage today)
- **As coordinator**, when I assign a cake vendor to a cake order, I want supplies (sugar, flour, fondant, etc.) to be auto-debited from stock so we don't double-allocate

### 3.4 Reporting (future, not MVP)
- **As founder**, I want to see procurement spend by category & month
- **As ops**, I want to see supplier reliability (% PO delivered on time)

---

## 4. Architecture

### 4.1 Service & Schema
- New microservice: `supply-service`
- Port: **8096**
- Database schema: `supply` (separate from `chefs`, `bookings` etc.)
- Java package: `com.safar.supply`
- Existing: 13 services (auth, user, listing, search, booking, payment, review, media, notification, messaging, chef, flight, api-gateway). Adds 14th.

### 4.2 Why a new service (not folded into chef-service)
- SCM cuts across cooks **and** PG **and** maintenance — doesn't belong inside any single domain
- Suppliers are a different actor than vendors/customers/cooks (different lifecycle, different KYC, different payment)
- Schema isolation: `chefs.partner_vendors` (service vendors who sell TO our customers) vs `supply.suppliers` (suppliers we buy FROM) — different concepts despite similar entity shape

### 4.3 Inter-service flows
```
       chef-service                supply-service
       ───────────                  ─────────────
EventBookingVendor.markDelivered ──Kafka──> stock.consume.requested
                                              ↓
                                            StockMovement (OUT)

StockMovement balance < reorder_point ──Kafka──> stock.low.detected
                                                    ↓
                                              notification-service
                                                    ↓
                                              Admin email + in-app
```

For MVP, the chef-service → supply-service consumption call is **synchronous REST** (simpler debug; Kafka can be added later).

---

## 5. Data Model

### 5.1 `supply.suppliers`
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| business_name | varchar(160) NOT NULL | |
| owner_name | varchar(120) | |
| phone | varchar(20) NOT NULL | |
| email | varchar(160) | |
| whatsapp | varchar(20) | |
| gst | varchar(20) | |
| pan | varchar(15) | |
| bank_account | varchar(40) | |
| bank_ifsc | varchar(15) | |
| bank_holder | varchar(120) | |
| address | text | |
| categories | text[] | which item categories they supply (GROCERY, BAKERY, DECOR, PG_LINEN, MAINTENANCE) |
| service_cities | text[] | cities they deliver to (empty = anywhere) |
| lead_time_days | int default 2 | typical delivery lead time |
| payment_terms | varchar(40) | NET_0 / NET_7 / NET_15 / NET_30 |
| credit_limit_paise | bigint | platform credit ceiling |
| kyc_status | varchar(20) | PENDING / VERIFIED / REJECTED |
| kyc_notes | text | |
| rating_avg | numeric(3,2) | based on PO on-time + quality |
| rating_count | int default 0 | |
| pos_completed | int default 0 | |
| active | boolean default true | |
| notes | text | admin-only |
| created_at, updated_at | timestamptz | |

Indexes: `(active)`, `(kyc_status)`, GIN on `categories`, GIN on `service_cities`.

### 5.2 `supply.supplier_catalog_items`
The price list a supplier offers. One row per supplier × item.

| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| supplier_id | UUID FK | |
| item_key | varchar(60) | e.g. `flour_maida`, `fondant_white`, `marigold_garland` (canonical) |
| item_label | varchar(120) | display name |
| category | varchar(30) | GROCERY / BAKERY / DECOR / PG_LINEN / MAINTENANCE |
| unit | varchar(20) | KG / GRAM / LITRE / PIECE / METRE / DOZEN |
| price_paise | bigint NOT NULL | per unit |
| moq_qty | numeric(12,2) | minimum order quantity |
| pack_size | numeric(12,2) | typical pack (e.g. flour comes in 25kg sacks) |
| lead_time_days | int | overrides supplier default if set |
| active | boolean default true | |
| notes | text | |
| created_at, updated_at | timestamptz | |

Unique constraint: `(supplier_id, item_key)`.

### 5.3 `supply.purchase_orders`
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| po_number | varchar(20) UNIQUE | auto-gen `PO-YYYYMM-NNNN` |
| supplier_id | UUID FK | |
| status | varchar(20) | DRAFT / ISSUED / ACKNOWLEDGED / IN_TRANSIT / DELIVERED / INVOICED / PAID / CANCELLED |
| ordered_at | timestamptz | when status moved to ISSUED |
| expected_delivery | date | |
| delivered_at | timestamptz | when status moved to DELIVERED |
| invoice_number | varchar(60) | supplier invoice ref |
| invoice_paise | bigint | invoice total (may differ from PO total) |
| invoiced_at | timestamptz | |
| paid_at | timestamptz | |
| payment_ref | varchar(60) | NEFT UTR |
| total_paise | bigint | sum of items at order time |
| tax_paise | bigint | GST (admin enters, default 18% of taxable) |
| grand_total_paise | bigint | total + tax |
| delivery_address | text | warehouse / drop point |
| created_by_user_id | UUID | admin who raised |
| admin_notes | text | |
| created_at, updated_at | timestamptz | |

Indexes: `(status)`, `(supplier_id, status)`, `(expected_delivery) WHERE status IN ('ISSUED','ACKNOWLEDGED','IN_TRANSIT')` for overdue detection.

### 5.4 `supply.purchase_order_items`
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| po_id | UUID FK | |
| catalog_item_id | UUID FK NULL | nullable — admin can add ad-hoc items not in catalog |
| item_key | varchar(60) | denormalised at PO time |
| item_label | varchar(120) | denormalised |
| category | varchar(30) | denormalised |
| unit | varchar(20) | denormalised |
| qty | numeric(12,2) NOT NULL | |
| unit_price_paise | bigint NOT NULL | denormalised |
| line_total_paise | bigint NOT NULL | qty × unit_price |
| received_qty | numeric(12,2) default 0 | partial-receive support |
| notes | text | |

Unique: `(po_id, item_key)`.

### 5.5 `supply.stock_items`
Current on-hand stock. One row per `item_key`. Updated by triggers from movements (or app-level).

| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| item_key | varchar(60) UNIQUE | |
| item_label | varchar(120) | |
| category | varchar(30) | |
| unit | varchar(20) | |
| on_hand_qty | numeric(12,2) default 0 | |
| reserved_qty | numeric(12,2) default 0 | allocated but not yet shipped to event |
| reorder_point | numeric(12,2) | threshold for low-stock alert |
| reorder_qty | numeric(12,2) | suggested PO qty when triggering reorder |
| last_unit_cost_paise | bigint | last received unit cost (for valuation) |
| last_received_at | timestamptz | |
| active | boolean default true | |
| created_at, updated_at | timestamptz | |

Indexes: `(category)`, `(on_hand_qty)` for low-stock query.

### 5.6 `supply.stock_movements`
Append-only ledger of every quantity change. Source of truth — `stock_items.on_hand_qty` is a denormalised cache.

| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| stock_item_id | UUID FK | |
| item_key | varchar(60) | denormalised |
| direction | varchar(10) | IN / OUT / ADJUSTMENT |
| qty | numeric(12,2) | always positive — sign comes from direction |
| reason | varchar(40) | PO_RECEIVED / EVENT_CONSUMED / ADJUSTMENT_DAMAGE / ADJUSTMENT_COUNT / RETURN |
| ref_type | varchar(30) | PO / EVENT_BOOKING / MANUAL |
| ref_id | UUID | id of source record |
| unit_cost_paise | bigint | for IN movements |
| performed_by_user_id | UUID | admin |
| notes | text | |
| created_at | timestamptz | |

Indexes: `(stock_item_id, created_at DESC)`, `(ref_type, ref_id)`.

---

## 6. APIs

All admin-only. JWT with `ROLE_ADMIN`. Base: `/api/v1/`.

### 6.1 Suppliers
| Method | Path | Purpose |
|---|---|---|
| GET | `/suppliers/admin` | list suppliers with filters (category, active, kyc) |
| GET | `/suppliers/admin/{id}` | detail |
| POST | `/suppliers/admin` | create |
| PUT | `/suppliers/admin/{id}` | update |
| POST | `/suppliers/admin/{id}/kyc` | verify / reject |
| POST | `/suppliers/admin/{id}/active?value=true|false` | toggle active |
| DELETE | `/suppliers/admin/{id}` | soft-delete (active=false) |

### 6.2 Supplier catalog
| Method | Path | Purpose |
|---|---|---|
| GET | `/suppliers/admin/{id}/catalog` | list catalog items |
| POST | `/suppliers/admin/{id}/catalog` | add catalog item |
| PUT | `/suppliers/admin/{id}/catalog/{itemId}` | update |
| DELETE | `/suppliers/admin/{id}/catalog/{itemId}` | soft-delete |

### 6.3 Purchase Orders
| Method | Path | Purpose |
|---|---|---|
| GET | `/purchase-orders/admin` | list with filters (status, supplier, date range) |
| GET | `/purchase-orders/admin/{id}` | detail with line items |
| POST | `/purchase-orders/admin` | create DRAFT PO |
| PUT | `/purchase-orders/admin/{id}` | update DRAFT |
| POST | `/purchase-orders/admin/{id}/issue` | DRAFT → ISSUED (sends email/WA to supplier) |
| POST | `/purchase-orders/admin/{id}/ack` | ISSUED → ACKNOWLEDGED |
| POST | `/purchase-orders/admin/{id}/in-transit` | ACKNOWLEDGED → IN_TRANSIT |
| POST | `/purchase-orders/admin/{id}/deliver` | IN_TRANSIT → DELIVERED (creates IN stock movements) |
| POST | `/purchase-orders/admin/{id}/invoice` | DELIVERED → INVOICED (records invoice) |
| POST | `/purchase-orders/admin/{id}/pay` | INVOICED → PAID (records UTR) |
| POST | `/purchase-orders/admin/{id}/cancel` | any → CANCELLED |

### 6.4 Stock
| Method | Path | Purpose |
|---|---|---|
| GET | `/stock/admin` | list stock items (filters: category, low-stock-only) |
| GET | `/stock/admin/{itemKey}` | detail with movement history |
| GET | `/stock/admin/movements` | global movement history with filters |
| POST | `/stock/admin/items` | create stock item |
| PUT | `/stock/admin/items/{itemKey}` | update reorder thresholds, label |
| POST | `/stock/admin/items/{itemKey}/adjust` | manual adjustment with reason |
| POST | `/stock/admin/consume` | (called by chef-service) record OUT for event/booking |

### 6.5 Internal (service-to-service)
| Method | Path | Purpose |
|---|---|---|
| POST | `/internal/stock/consume` | chef-service calls when vendor delivers — body: `{ items: [{itemKey, qty}], refType, refId }` |

---

## 7. Admin UI

### 7.1 Suppliers (`/suppliers`)
Mirrors `PartnerVendorsPage` — list, search, filter by category, add/edit modal with bank/KYC/categories/cities, KYC drawer.

### 7.2 Supplier Catalog (`/suppliers/{id}/catalog`)
Inside the supplier drawer/page: editable table of catalog items with item_key, label, category, unit, price, MOQ.

### 7.3 Purchase Orders (`/purchase-orders`)
- Top: stat cards — Open POs, Overdue, This Month Spend, Pending Payment
- Table: PO#, supplier, status chip, items count, total, expected delivery, actions
- Create PO modal: pick supplier → loads catalog → multi-add items with qty → auto-calculates total/tax → expected delivery date → admin notes → "Save as Draft" or "Issue Now"
- Detail drawer: line items, status timeline, action buttons per status, invoice/UTR fields

### 7.4 Stock (`/stock`)
- Tabs per category (Grocery / Bakery / Decor / PG Linen / Maintenance)
- Per tab: table with item, on-hand qty, reorder point, status badge (OK / LOW / OUT)
- Low-stock items get "Create PO" button → pre-filled PO modal
- Movement history drawer when clicking an item

---

## 8. Integration with Existing Marketplace

### 8.1 Cake order flow (Phase 1 wired)
1. Customer orders 2kg vanilla cake → `EventBooking` created (chef-service) with `menuDescription.type=DESIGNER_CAKE`
2. Admin assigns cake vendor (existing PartnerVendor flow)
3. Vendor delivers cake → admin clicks "Mark delivered" on event row
4. chef-service → POST to supply-service `/internal/stock/consume`:
   ```json
   { "refType": "EVENT_BOOKING", "refId": "<bookingId>",
     "items": [
       { "itemKey": "flour_maida", "qty": 0.5 },
       { "itemKey": "sugar_white", "qty": 0.4 },
       { "itemKey": "fondant_white", "qty": 0.3 }
     ]}
   ```
5. supply-service creates 3 OUT stock movements, decrements on_hand_qty
6. If any item drops below reorder_point → emit `stock.low.detected` Kafka event

For Phase 1, the **bill of materials (BOM)** for each bespoke service is **hardcoded as a constant** in chef-service. Phase 2 will move BOMs to a configurable table.

### 8.2 Decor order flow (Phase 1 wired)
Same pattern. BOM for "Birthday Premium" decor consumes flowers, balloons, ribbons.

### 8.3 PG check-in (Phase 2)
On tenancy start, OUT stock movement for linen kit + toiletries.

### 8.4 Maintenance ticket (Phase 2)
Maintenance-service issues OUT for paint, pipes, electrical etc.

---

## 9. Phasing

### Phase 1 — MVP (this build)
- supply-service skeleton (port 8096, schema `supply`)
- V1 schema (6 tables)
- Suppliers + catalog CRUD (admin only)
- PO CRUD + lifecycle + auto-stock-IN on deliver
- Stock items + movements + manual adjustments
- Low-stock query (no scheduler yet — admin-driven check)
- Admin pages: Suppliers, Purchase Orders, Stock
- Cake + decor BOM consumption hooked from chef-service via REST (synchronous)

### Phase 2
- Kafka event flow (`stock.consume.requested`, `stock.low.detected`)
- Notification-service consumer for low-stock alerts (email + in-app)
- Email/WhatsApp PO copy to supplier on ISSUED transition
- Configurable BOM table (no more hardcoded constants)
- Supplier rating from PO delivery on-time %
- PG and maintenance integration

### Phase 3
- Supplier self-service portal (acknowledge POs, mark in-transit)
- Auto-reorder (cron-based PO suggestions when reorder_point hit)
- Multi-warehouse / location tracking
- Razorpay payouts to supplier (currently NEFT manual)
- Demand forecasting from booking pipeline

---

## 10. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| BOM hardcoded → drift from real recipe | High | Medium | Phase 2 moves to DB; for MVP, only cake + decor wired (small surface) |
| Stock decrements on bad data → negative on_hand_qty | Medium | High | Allow but flag in red; admin can adjust. Don't block movements on validation. |
| Supplier doesn't acknowledge PO → status stuck ISSUED | High | Low | Admin can manually advance status; nightly job alerts on >48hr stuck POs (Phase 2) |
| Two admins create same PO simultaneously | Low | Low | DB-level unique on po_number; client error on retry |
| Service-to-service REST coupling fragile | Medium | Medium | MVP keeps it sync for debug ease; Kafka migration is Phase 2 |

---

## 11. Success Metrics (90 days post-launch)

- ≥ 80% of cake & decor orders trigger stock movements (i.e. SCM is live and consuming)
- ≥ 5 active suppliers per metro
- < 5% of POs in stuck-status >7 days
- ≥ 70% of low-stock alerts result in PO within 24h
- Stock-out incidents reduce vs baseline (qualitative — log via Slack channel)

---

## 12. Open Questions

1. **Where does the platform's own warehouse physically live?** MVP assumes virtual single warehouse. Real ops may need vendor-direct ship (supplier ships to vendor, not us). Defer until we have signal.
2. **Tax handling on POs** — GST input credit reconciliation? MVP just records; bookkeeping stays in Tally/spreadsheet for now.
3. **Returns flow** — supplier sends bad goods back. Defer.
4. **Service-vendor (PartnerVendor) vs supplier overlap** — some PartnerVendors may also be suppliers (a baker sells cakes AND supplies fondant). Two separate entities for v1; merge later if pattern emerges.

---

## 13. Out-of-band ops (until Phase 2 lands)
- Supplier coordination via WhatsApp (admin manually shares PO PDF for now)
- Payouts via NEFT (admin enters UTR after transfer)
- Low-stock alerts visible in admin only (no email/SMS until Phase 2)
- BOM updates require code change to chef-service constants

---

## 14. Appendix: BOM constants (initial)

```
DESIGNER_CAKE per kg:
  flour_maida    : 0.50 kg
  sugar_white    : 0.40 kg
  butter         : 0.20 kg
  eggs           : 4 PIECE
  fondant_white  : 0.30 kg
  cream_dairy    : 0.20 LITRE

EVENT_DECOR per booking ("Standard"):
  marigold_garland : 6 PIECE
  rose_loose       : 1 KG
  balloon_helium   : 30 PIECE
  ribbon_satin     : 10 METRE
  candle_pillar    : 6 PIECE
```

These ship in the Phase 1 chef-service constants file `BespokeBom.java`.
