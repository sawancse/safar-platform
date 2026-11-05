# =============================================================================
#  startup-boot.ps1  —  Auto-start wrapper run by Task Scheduler at Windows logon
# -----------------------------------------------------------------------------
#  Waits for the Docker Desktop daemon to be ready (it is NOT up yet at logon),
#  then hands off to start-all.ps1 which brings up the full stack:
#    infra (Postgres/Redis/Kafka/Zookeeper/Elasticsearch) -> Java services
#    -> AI + gateway -> safar-web + admin -> Cloudflare tunnel.
#
#  Registered via: schtasks /create /tn SafarAutoStart /sc ONLOGON ...
#  Manual run:     .\startup-boot.ps1            (same as a fresh boot)
#                  .\startup-boot.ps1 -Dev       (pass-through to start-all.ps1)
# =============================================================================
param(
  [switch]$Dev,
  [switch]$NoTunnel,
  [switch]$CoreOnly
)

$Platform  = 'C:\Users\Win-10\safar-platform'
$LogDir    = "$Platform\logs"
$LogFile   = "$LogDir\startup-boot.log"
$DockerExe = 'C:\Program Files\Docker\Docker\Docker Desktop.exe'

if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Path $LogDir -Force | Out-Null }

function Log($msg) {
  $line = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  $msg"
  Write-Host $line
  Add-Content -Path $LogFile -Value $line
}

Log "==== SafarAutoStart boot ===="

# ---- 1. Make sure the Docker Desktop app is running ----
if (-not (Get-Process 'Docker Desktop' -ErrorAction SilentlyContinue)) {
  Log "Docker Desktop not running - launching it."
  if (Test-Path $DockerExe) {
    Start-Process $DockerExe
  } else {
    Log "WARNING: Docker Desktop.exe not found at $DockerExe"
  }
} else {
  Log "Docker Desktop process already running."
}

# ---- 2. Wait for the Docker ENGINE (daemon) to actually accept commands ----
# The app process starting != the Linux engine being ready. Poll `docker info`.
Log "Waiting for Docker engine to become ready (up to 5 min)..."
$dockerReady = $false
foreach ($i in 1..60) {
  docker info *> $null
  if ($LASTEXITCODE -eq 0) {
    $dockerReady = $true
    Log "Docker engine ready (after ~$($i*5)s)."
    break
  }
  Start-Sleep -Seconds 5
}
if (-not $dockerReady) {
  Log "ERROR: Docker engine not ready after 5 min. Aborting - start manually once Docker is up."
  exit 1
}

# ---- 3. Hand off to the full stack orchestrator ----
$fwd = @()
if ($Dev)      { $fwd += '-Dev' }
if ($NoTunnel) { $fwd += '-NoTunnel' }
if ($CoreOnly) { $fwd += '-CoreOnly' }
Log "Launching start-all.ps1 $($fwd -join ' ')"

try {
  & "$Platform\start-all.ps1" @fwd
  Log "start-all.ps1 returned (tabs launched)."
} catch {
  Log "ERROR from start-all.ps1: $_"
  exit 1
}

Log "==== SafarAutoStart boot complete ===="
