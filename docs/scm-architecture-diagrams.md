# Supply Chain — Architecture Diagrams

Three views: **end-to-end flow**, **domain entities**, **adapter framework**.

All diagrams are mermaid — render at https://mermaid.live or in any GitHub-flavoured-markdown viewer.

---

## 1. End-to-end flow: customer order → vendor fulfils → stock debits → PO replenishes

```mermaid
sequenceDiagram
    autonumber
    participant C as Customer (safar-web)
    participant CS as chef-service<br/>(EventBooking)
    participant Adm as Admin<br/>(CooksPage / SuppliersPage)
    participant SS as supply-service<br/>(StockService)
    participant V as Partner Vendor<br/>(cake / decor / pandit / ...)
    participant Sup as External Supplier<br/>(Udaan / FNP / Manual)
    participant K as Kafka outbox

    Note over C,V: A. Customer orders a 2kg cake → vendor fulfils → stock debits
    C->>CS: POST /chef-events { type: DESIGNER_CAKE, breakdown }
    CS-->>C: EventBooking { id, advance ₹X }
    C->>CS: pay advance (Razorpay)
    CS->>K: emit booking.advance-paid

    Adm->>CS: GET events tab → assign cake vendor
    CS->>K: emit vendor.assigned (notify-service consumes)
    CS-->>Adm: EventBookingVendor (status=ASSIGNED)
    V->>Adm: delivers cake on event day
    Adm->>CS: POST /vendor/{id}/delivered
    CS->>SS: POST /internal/stock/consume<br/>(BOM: flour 0.5kg, sugar 0.4kg, fondant 0.3kg)
    SS->>SS: -3 stock_movements (OUT)<br/>StockItem.onHandQty −= qty
    SS-->>CS: 200 OK

    Note over Adm,Sup: B. Stock dropped below reorder_point → admin raises PO
    Adm->>SS: GET /stock?lowOnly=true
    SS-->>Adm: [flour: 18kg ↓ reorder=20]
    Adm->>SS: POST /purchase-orders { supplier=FreshKart, line: flour×50 }
    SS-->>Adm: PO-202604-1234 (DRAFT)
    Adm->>SS: POST /purchase-orders/{id}/issue
    SS->>SS: validateTransition(DRAFT→ISSUED)
    SS->>Sup: adapter.placePo()<br/>(MANUAL: no-op / UDAAN: API call)
    Sup-->>SS: external_ref or "MANUAL-PO-..."
    SS-->>Adm: status=ISSUED, externalRef set

    Sup->>SS: status updates (poll job for UDAAN, manual for MANUAL)
    SS->>SS: transition(IN_TRANSIT → DELIVERED)
    SS->>SS: receive() → +stock_movements (IN)<br/>StockItem.onHandQty += qty
    Adm->>SS: record invoice + UTR
    SS->>SS: status: INVOICED → PAID
```

---

## 2. Domain entities + relationships

```mermaid
erDiagram
    %% chef-service domain (services we sell TO customers)
    EVENT_BOOKING ||--o{ EVENT_BOOKING_VENDOR : "has assignment"
    EVENT_BOOKING_VENDOR }o--|| PARTNER_VENDOR : "assigned to"
    PARTNER_VENDOR {
        uuid id PK
        enum service_type "CAKE_DESIGNER / EVENT_DECOR / PANDIT_PUJA / LIVE_MUSIC / APPLIANCE_RENTAL / STAFF_HIRE"
        string business_name
        text[] service_cities
        string kyc_status
        decimal rating_avg
    }
    EVENT_BOOKING_VENDOR {
        uuid id PK
        uuid event_booking_id FK
        uuid vendor_id FK
        enum status "ASSIGNED / CONFIRMED / DELIVERED / CANCELLED"
        long payout_paise
        string payout_status
        string payout_ref
    }
    EVENT_BOOKING {
        uuid id PK
        text menu_description "JSON: type=CAKE_DESIGNER, breakdown"
        long total_amount_paise
        enum status
    }

    %% supply-service domain (suppliers we buy FROM)
    SUPPLIER ||--o{ SUPPLIER_CATALOG_ITEM : "lists"
    SUPPLIER ||--o{ PURCHASE_ORDER : "fulfils"
    PURCHASE_ORDER ||--o{ PURCHASE_ORDER_ITEM : "has lines"
    PURCHASE_ORDER_ITEM }o--o| SUPPLIER_CATALOG_ITEM : "from catalog"
    PURCHASE_ORDER ||--o{ STOCK_MOVEMENT : "creates IN movements"

    SUPPLIER {
        uuid id PK
        string business_name
        text[] categories "GROCERY / BAKERY / DECOR / PG_LINEN / MAINTENANCE"
        text[] service_cities
        enum integration_type "MANUAL / UDAAN / FNP / AMAZON_BUSINESS / ..."
        jsonb integration_config
        string kyc_status
        timestamptz catalog_synced_at
    }
    SUPPLIER_CATALOG_ITEM {
        uuid id PK
        uuid supplier_id FK
        string item_key "snake_case canonical (flour_maida)"
        enum category
        enum unit
        long price_paise
        decimal moq_qty
    }
    PURCHASE_ORDER {
        uuid id PK
        string po_number "PO-YYYYMM-NNNN"
        uuid supplier_id FK
        enum status "DRAFT → ISSUED → ACK → IN_TRANSIT → DELIVERED → INVOICED → PAID"
        date expected_delivery
        long grand_total_paise
        string external_ref "supplier order id"
        string external_status
        text external_error
    }
    PURCHASE_ORDER_ITEM {
        uuid id PK
        uuid po_id FK
        uuid catalog_item_id FK
        string item_key "denormalised"
        decimal qty
        long unit_price_paise
        decimal received_qty
    }

    %% stock domain (cross-cutting)
    STOCK_ITEM ||--o{ STOCK_MOVEMENT : "has history"
    STOCK_ITEM {
        uuid id PK
        string item_key UK "flour_maida"
        enum category
        enum unit
        decimal on_hand_qty "denormalised cache"
        decimal reorder_point
        long last_unit_cost_paise
    }
    STOCK_MOVEMENT {
        uuid id PK
        uuid stock_item_id FK
        enum direction "IN / OUT / ADJUSTMENT"
        decimal qty
        string reason "PO_RECEIVED / EVENT_CONSUMED / ADJUSTMENT_*"
        string ref_type "PO / EVENT_BOOKING / MANUAL"
        uuid ref_id
        long unit_cost_paise
    }

    %% Cross-service link (no FK — chef-service calls supply via REST)
    EVENT_BOOKING ||..|| STOCK_MOVEMENT : "consume on vendor delivered (refType=EVENT_BOOKING)"
```

**Key relationships:**
- `partner_vendors` (chef-service, schema `chefs`) sell **TO** customers via `event_booking_vendor` join
- `suppliers` (supply-service, schema `supply`) sell **TO** the platform via `purchase_orders`
- They are **separate entities by design** — same shape, different actors, different schemas
- `stock_movements` is the source of truth; `stock_items.on_hand_qty` is a denormalised cache
- Cross-service ref: `EventBooking` consumes stock via REST call to `/internal/stock/consume`; the resulting `stock_movement.ref_id` points back to the booking id

---

## 3. Adapter framework architecture

```mermaid
flowchart TD
    subgraph supply["supply-service"]
        direction TB

        subgraph statemachine["PO state machine"]
            POS["PurchaseOrderService.transition()"]
            POS -->|"on ISSUED"| disp["dispatchToSupplier()"]
            POS -->|"on DELIVERED"| recv["receiveStockForPo()"]
        end

        subgraph reg["Adapter Registry"]
            REG["SupplierAdapterRegistry<br/>@PostConstruct indexes by IntegrationType"]
        end

        subgraph adapters["Adapters (implement SupplierAdapter)"]
            direction TB
            MAN["ManualSupplierAdapter<br/>✓ live (no-op)"]
            UDA["UdaanSupplierAdapter<br/>⚠ stub: TODO blocks"]
            FNP["FnpSupplierAdapter<br/>⚠ stub: TODO blocks"]
            future1["AmazonBusinessAdapter<br/>(future)"]
        end

        subgraph jobs["Scheduled jobs (gated on supply.adapters.enabled)"]
            CSJ["CatalogSyncJob<br/>@Scheduled cron 03:00 IST<br/>skips MANUAL"]
            PSJ["PoStatusPollJob<br/>@Scheduled fixedRate 15min<br/>skips MANUAL"]
        end

        flag{"supply.adapters.enabled?"}

        disp --> flag
        flag -->|false| done1["return (no-op)"]
        flag -->|true| REG
        REG -->|"forSupplier(supplierId)<br/>→ supplier.integrationType"| picker

        picker{"integrationType?"}
        picker -->|MANUAL| MAN
        picker -->|UDAAN| UDA
        picker -->|FNP| FNP

        CSJ --> REG
        PSJ --> REG

        recv --> stock["StockService.receive()<br/>+ stock_movements (IN)"]
    end

    subgraph external["External APIs"]
        direction TB
        UdaanAPI["Udaan B2B API<br/>POST /v1/orders<br/>GET /v1/orders/{id}<br/>GET /v1/catalog"]
        FnpAPI["FNP B2B API<br/>POST /b2b/v2/orders<br/>X-FNP-Partner-Id header"]
    end

    UDA -.->|"TODO: real RestTemplate"| UdaanAPI
    FNP -.->|"TODO: real RestTemplate"| FnpAPI

    UDA -.->|"throw SupplierAdapterException<br/>not implemented"| FAIL["fail-soft<br/>po.externalError set<br/>PO stays ISSUED"]
    FNP -.->|"throw SupplierAdapterException"| FAIL

    MAN -.->|"return MANUAL-PO-..."| OK1["po.externalRef set<br/>continues normally"]

    style MAN fill:#90EE90
    style UDA fill:#FFE4B5
    style FNP fill:#FFE4B5
    style future1 fill:#D3D3D3,stroke-dasharray:5
    style flag fill:#FFD700
    style FAIL fill:#FFB6B6
    style OK1 fill:#B6FFB6
```

**Read this diagram as:**
- Yellow diamond is the **master switch** (`supply.adapters.enabled`). When false (default today), the adapter call short-circuits and PO behaves exactly like Phase 1.
- Green block (`ManualSupplierAdapter`) is the **only adapter that's live**. It returns a synthetic `MANUAL-{poNumber}` ref so callers don't special-case "no integration".
- Beige blocks (`Udaan` / `Fnp`) are **stubs** — Spring beans wired correctly, status mapping done, but every HTTP call site throws `SupplierAdapterException("Not implemented")`. Filling them in is ~50 lines of `RestTemplate` per method, gated on real partner sandbox creds.
- Red block (`fail-soft`) shows the safety net: if any adapter throws, the PO stays `ISSUED` locally with `external_error` captured. **No transition ever fails because of an adapter.** Admin sees the error in the drawer and can retry.
- The two `@Scheduled` jobs (catalog sync + status poll) skip `MANUAL` suppliers entirely — they only matter once a real integration goes live.

---

## Bonus: PO state machine (close-up)

```mermaid
stateDiagram-v2
    [*] --> DRAFT: create PO
    DRAFT --> ISSUED: issue (+ adapter.placePo)
    DRAFT --> CANCELLED: cancel

    ISSUED --> ACKNOWLEDGED: supplier ack
    ISSUED --> IN_TRANSIT: skip ack (allowed)
    ISSUED --> DELIVERED: skip ack+transit (fast supplier)
    ISSUED --> CANCELLED: cancel (+ adapter.cancelPo)

    ACKNOWLEDGED --> IN_TRANSIT: dispatched
    ACKNOWLEDGED --> DELIVERED: skip transit
    ACKNOWLEDGED --> CANCELLED: cancel

    IN_TRANSIT --> DELIVERED: arrived (+ stock IN movements)
    IN_TRANSIT --> CANCELLED: cancel

    DELIVERED --> INVOICED: record supplier invoice
    INVOICED --> PAID: NEFT UTR recorded

    PAID --> [*]
    CANCELLED --> [*]

    note right of DELIVERED
        receiveStockForPo()
        +stock_movements (IN, reason=PO_RECEIVED)
        StockItem.onHandQty += qty
        StockItem.lastUnitCostPaise = unit_price
        Supplier.posCompleted++
    end note

    note right of ISSUED
        dispatchToSupplier() [if adapters.enabled]
        adapter.placePo()
        po.externalRef = supplier order id
        on failure: po.externalError set, PO stays ISSUED
    end note
```

---

## Notes on rendering

- These are mermaid-flavoured markdown — render at https://mermaid.live (paste each block) or in any GFM viewer (GitHub PR preview, Cursor preview, Notion, Obsidian)
- For a static export, use the mermaid CLI: `npx -p @mermaid-js/mermaid-cli mmdc -i scm-architecture-diagrams.md -o scm-diagrams.png`
- For inclusion in a slide deck, copy each block to https://mermaid.live and export PNG/SVG

## Where this connects

- PRD: `prd-supply-chain.md` (the "what" + Phase 1/2/3 split)
- Adapter design: `supplier-adapter-design.md` (deeper interface contract + sequence diagrams)
- Partnership runbook: `supplier-partnership-checklist.md` (how to get sandbox creds)
