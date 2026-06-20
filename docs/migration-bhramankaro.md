# Domain Migration: ysafar.com → bhramankaro.com (laptop server + Cloudflare Tunnel)

**Status:** Live as of 2026-06-20. bhramankaro.com is served from the local laptop
via a dedicated Cloudflare tunnel, fully independent of ysafar.com.

## Target architecture
```
                 ┌─ bhramankaro.com ──────► localhost:3000  (safar-web / Next.js)
Cloudflare DNS ──┼─ www.bhramankaro.com ──► localhost:3000
 (bhramankaro    ├─ api.bhramankaro.com ──► localhost:8080  (api-gateway)
  zone)          └─ admin.bhramankaro.com ► localhost:3001  (admin / Vite)
        │
        └─ tunnel: bhraman-local (id 1813756a-2bc6-4e5c-a9d2-1778770fffd2)

Laptop runs the whole stack:
  Docker: Postgres 16 · Redis 7 · Kafka+Zookeeper · Elasticsearch 8
  ~15 Java services · AI service (8090) · Next.js (3000) · Admin (3001)
```

## What is independent from ysafar (nothing shared on the serving path)
| Piece | ysafar.com | bhramankaro.com |
|-------|-----------|-----------------|
| Cert  | `~/.cloudflared/cert-ysafar.pem` (backup) | `~/.cloudflared/cert.pem` |
| Tunnel | `safar-local` / `ysafartunnel` | **`bhraman-local`** (`1813756a-…`) |
| Tunnel creds | `79243f91-….json` | `1813756a-….json` |
| Config | `config.ysafar.bak.yml` | `config.yml` |
| DNS   | ysafar zone | bhramankaro zone (4 proxied CNAMEs) |

## Cloudflare DNS (bhramankaro.com zone) — 4 records
All **CNAME, Proxied (orange cloud)** → `1813756a-2bc6-4e5c-a9d2-1778770fffd2.cfargotunnel.com`
- `@`  ·  `www`  ·  `api`  ·  `admin`

> The tunnel was created with the existing (ysafar-scoped) cert because tunnel
> creation is account-scoped. DNS records were added **manually** in the dashboard
> rather than via `cloudflared tunnel route dns`, because that cert is zone-scoped
> to ysafar and would otherwise create junk `*.bhramankaro.com.ysafar.com` records
> in the wrong zone. No re-login was needed.

## Code changes (done + pushed)
Swept `ysafar.com → bhramankaro.com` across both repos (brand name "Safar" untouched):
- **safar-web** commit `fd305f5` (23 files) — `.env.production` API host, `next.config.mjs`
  image domains, SEO metadata, sitemap/robots, JSON-LD, legal/help pages.
- **safar-platform** commit `5992069` (68 files) — gateway/listing CORS, notification
  base-url + ~40 email templates, mobile store listings, terraform, docs, env.
- Build-time env: `NEXT_PUBLIC_*` (web) and `VITE_*` (admin) are baked at **build**,
  so the frontends must be **built** (not `dev`) for external tunnel users to hit
  `https://api.bhramankaro.com`. `start-all.ps1` (no `-Dev`) does this.

## Running the stack
```powershell
cd C:\Users\Win-10\safar-platform
.\start-all.ps1            # full prod-like stack + tunnel
.\start-all.ps1 -CoreOnly  # lighter (essential services only)
.\start-all.ps1 -Dev       # localhost API (local browser only, no external)
.\start-all.ps1 -NoTunnel  # don't start cloudflared
```
Verify:
```
https://bhramankaro.com
https://api.bhramankaro.com/api/v1/listings
https://admin.bhramankaro.com
```

## ⚠️ Known issue — cloudflared auto-start service runs the WRONG tunnel
The installed Windows service `Cloudflared` starts with a **`--token`** for tunnel
`e60106ff-cbd9-4a61-abd7-fcf168540484` (account `2d12c3b5…`) — a different,
dashboard-managed tunnel, **not** `bhraman-local`. So today bhramankaro.com is
served by a **manual** `cloudflared tunnel run bhraman-local` process that dies on
reboot/logoff. To make it survive reboots, do ONE of:
- **Scheduled Task** (recommended, non-destructive): create a task that runs
  `cloudflared tunnel run bhraman-local` at logon. Leaves the existing service alone.
- **Re-point the service**: `cloudflared service uninstall` then re-install so it
  uses `config.yml` (which points at bhraman-local). Only do this if the
  token tunnel `e60106ff` is confirmed dead/unneeded.

## Remaining external configs (so login/payments work on new domain)
- **Google OAuth** (client `819322140862-…`): add `https://bhramankaro.com` to
  Authorized JavaScript origins + redirect URIs.
- **Razorpay**: authorized domains + webhook → `https://api.bhramankaro.com/api/v1/payments/webhook`.
- **Duffel / WhatsApp** webhooks → `api.bhramankaro.com`.
- **Email deliverability**: SMTP is Gmail (`sawank.sit@gmail.com`) so mail still
  sends; for `@bhramankaro.com` sender add SPF/DKIM/DMARC in Cloudflare DNS.
- **media.bhramankaro.com**: add CNAME → S3/CloudFront (S3 preserved) if needed.
- **SEO**: add bhramankaro.com to Search Console + GA4; optional 301 from ysafar.

## Cleanup loose ends
- Delete the 4 junk records in the **ysafar.com** zone:
  `bhramankaro.com.ysafar.com`, `www.…`, `api.…`, `admin.bhramankaro.com.ysafar.com`.
- Optional: remove the leftover `"ysafar"` keyword in safar-web homepage `<meta keywords>`.
- Optional: upgrade cloudflared (current 2026.3.0 → 2026.6.1).

## Operational notes
- Laptop = uptime: site is down when it sleeps/reboots/loses internet.
  `powercfg /change standby-timeout-ac 0` to prevent sleep.
- The tunnel is outbound — dynamic home IP is fine, no port-forwarding.
- Only the 4 hostnames in `config.yml` ingress are exposed; DB/Kafka stay private.
- Consider Cloudflare Access (email-gated) in front of `admin.bhramankaro.com`.
