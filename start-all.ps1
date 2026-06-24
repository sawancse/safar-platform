# =============================================================================
#  start-all.ps1  —  Bring up the full Safar stack on this laptop (server mode)
#  Domain: bhramankaro.com  |  Tunnel: bhraman-local (Cloudflare)
# -----------------------------------------------------------------------------
#  Usage:
#    .\start-all.ps1                # full prod-like stack + tunnel (external users)
#    .\start-all.ps1 -Dev           # web/admin in dev mode (localhost API only)
#    .\start-all.ps1 -NoTunnel      # skip starting cloudflared
#    .\start-all.ps1 -CoreOnly      # only the essential services (lighter on RAM)
#
#  Prereqs: Docker Desktop running, Java 17 + Maven, Node, Python+uvicorn on PATH.
#  NOTE: One laptop running ~15 JVMs + Kafka + ES + Postgres + Next is RAM-heavy
#        (16-32 GB). Use -CoreOnly if it thrashes.
# =============================================================================
param(
  [switch]$Dev,
  [switch]$NoTunnel,
  [switch]$CoreOnly
)

$ErrorActionPreference = 'Stop'
$Platform = 'C:\Users\Win-10\safar-platform'
$Web      = 'C:\Users\Win-10\safar-web'
$Admin    = "$Platform\admin"

function Tab($title, $dir, $cmd) {
  # All tabs land in a single Windows Terminal window named "safar"
  wt.exe -w safar new-tab --title $title -d $dir powershell -NoExit -Command $cmd
  Start-Sleep -Milliseconds 600
}

Write-Host "==> 1/5  Infrastructure (Postgres/Redis/Kafka/Elasticsearch)" -ForegroundColor Cyan
Push-Location $Platform
docker compose up -d
Pop-Location

# Kafka MUST be accepting on 9092 before any Java service starts: a Spring-Kafka
# client that connects to a dead localhost:9092 can self-connect and lock the port
# against everything else (see memory: kafka_self_connect_504). Actively poll rather
# than guessing a fixed sleep.
Write-Host "    waiting for Kafka to accept on 9092..." -ForegroundColor DarkGray
$kafkaUp = $false
foreach ($i in 1..60) {
  try {
    $c = New-Object System.Net.Sockets.TcpClient
    $c.Connect('127.0.0.1', 9092)
    $c.Close()
    $kafkaUp = $true
    Write-Host "    Kafka up on 9092 (after ~$($i*2)s)" -ForegroundColor DarkGray
    break
  } catch {
    Start-Sleep -Seconds 2
  }
}
if (-not $kafkaUp) {
  Write-Host "    WARNING: Kafka not reachable on 9092 after 120s - starting services anyway (they may self-connect; check 'docker logs safar-kafka')." -ForegroundColor Yellow
}
Start-Sleep -Seconds 3  # let other infra (Postgres/Redis/ES) finish settling

# ---- Java services (module dir -> mvn spring-boot:run). Gateway started LAST. ----
$core = @(
  @{ n='auth';         d='auth-service' },
  @{ n='user';         d='user-service' },
  @{ n='listing';      d='listing-service' },
  @{ n='search';       d='search-service' },
  @{ n='booking';      d='booking-service' },
  @{ n='payment';      d='payment-service' },
  @{ n='notification'; d='notification-service' }
)
$extra = @(
  @{ n='review';     d='review-service' },
  @{ n='media';      d='media-service' },
  @{ n='messaging';  d='messaging-service' },
  @{ n='services';   d='services-service' },
  @{ n='flight';     d='flight-service' },
  @{ n='supply';     d='supply-service' },
  @{ n='insurance';  d='insurance-service' }
)
$services = if ($CoreOnly) { $core } else { $core + $extra }

Write-Host "==> 2/5  Java services ($($services.Count))" -ForegroundColor Cyan
foreach ($s in $services) {
  Tab $s.n "$Platform\services\$($s.d)" "mvn spring-boot:run"
}

Write-Host "==> 3/5  AI service (8090) + API Gateway (8080, last)" -ForegroundColor Cyan
Tab 'ai-service' "$Platform\services\ai-service" "uvicorn main:app --host 0.0.0.0 --port 8090 --reload"
Start-Sleep -Seconds 5
Tab 'gateway' "$Platform\services\api-gateway" "mvn spring-boot:run"

Write-Host "==> 4/5  Frontends (web 3000, admin 3001)" -ForegroundColor Cyan
if ($Dev) {
  # Dev mode: uses .env.local -> http://localhost:8080 (browser must be local)
  Tab 'web'   $Web   "npm run dev"
  Tab 'admin' $Admin "npm run dev"
} else {
  # Prod mode: builds with .env.production -> https://api.bhramankaro.com
  # (NEXT_PUBLIC_*/VITE_* are baked at build time, so a build is required for
  #  external users hitting the tunnel.)
  Tab 'web'   $Web   "npm run build; if (`$?) { npm start }"
  Tab 'admin' $Admin "npm run build; if (`$?) { npm run preview }"
}

if (-not $NoTunnel) {
  Write-Host "==> 5/5  Cloudflare tunnel (bhraman-local)" -ForegroundColor Cyan
  Tab 'tunnel' "$env:USERPROFILE\.cloudflared" "cloudflared tunnel run bhraman-local"
} else {
  Write-Host "==> 5/5  Tunnel skipped (-NoTunnel)" -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "All tabs launched in the 'safar' Windows Terminal window." -ForegroundColor Green
Write-Host "Give it 1-2 min, then verify:" -ForegroundColor Green
Write-Host "  https://bhramankaro.com  |  https://api.bhramankaro.com/api/v1/listings  |  https://admin.bhramankaro.com"
