#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────
# One-shot Ubuntu dev environment setup for the Safar platform.
# Idempotent — safe to re-run. Skips anything already installed.
#
# Usage (after booting into Ubuntu):
#     curl -sSL https://raw.githubusercontent.com/sawancse/safar-platform/master/scripts/setup-ubuntu-dev.sh | bash
#   OR after cloning:
#     bash scripts/setup-ubuntu-dev.sh
#
# Tested on: Ubuntu 22.04 LTS, 24.04 LTS
# ──────────────────────────────────────────────────────────────────────
set -euo pipefail

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
say()  { echo -e "${GREEN}[setup]${NC} $*"; }
warn() { echo -e "${YELLOW}[warn]${NC}  $*"; }
fail() { echo -e "${RED}[fail]${NC}  $*"; exit 1; }
have() { command -v "$1" &> /dev/null; }

# ── 0. Sanity ─────────────────────────────────────────────────────────
[[ "$(uname -s)" == "Linux" ]] || fail "This script is for Linux (Ubuntu)."
[[ "$(id -u)" -ne 0 ]] || fail "Run as your normal user, not root. Script will sudo when needed."
have sudo || fail "sudo not installed."

say "Updating apt..."
sudo apt-get update -qq

say "Installing base tools (curl, git, build-essential, ca-certificates)..."
sudo apt-get install -y -qq curl git build-essential ca-certificates gnupg lsb-release software-properties-common unzip

# ── 1. JDK 17 (Eclipse Temurin) ───────────────────────────────────────
if have javac && javac -version 2>&1 | grep -q "17\."; then
  say "JDK 17 already installed: $(javac -version 2>&1)"
else
  say "Installing OpenJDK 17..."
  sudo apt-get install -y -qq openjdk-17-jdk
  java -version
fi

# ── 2. Maven ──────────────────────────────────────────────────────────
if have mvn; then
  say "Maven already installed: $(mvn -v | head -1)"
else
  say "Installing Maven..."
  sudo apt-get install -y -qq maven
  mvn -v | head -1
fi

# ── 3. Node 20 via nvm ────────────────────────────────────────────────
export NVM_DIR="$HOME/.nvm"
if [[ ! -d "$NVM_DIR" ]]; then
  say "Installing nvm..."
  curl -sSL https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
  # shellcheck disable=SC1091
  [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
else
  say "nvm already installed."
  # shellcheck disable=SC1091
  [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
fi

if ! nvm ls 20 &> /dev/null; then
  say "Installing Node 20..."
  nvm install 20
fi
nvm use 20
say "Node $(node -v) / npm $(npm -v)"

# ── 4. Docker CE + compose plugin (proper Docker, not docker.io) ──────
if have docker; then
  say "Docker already installed: $(docker --version)"
else
  say "Installing Docker CE..."
  sudo install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  sudo chmod a+r /etc/apt/keyrings/docker.gpg
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
    sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
  sudo apt-get update -qq
  sudo apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  docker --version
fi

if ! id -nG "$USER" | grep -qw docker; then
  say "Adding $USER to docker group (you must log out + back in for this to take effect)..."
  sudo usermod -aG docker "$USER"
  warn "→ Log out + log back in (or reboot) before running docker commands without sudo."
fi

# ── 5. Postgres + Redis client tools (servers run via Docker) ─────────
if ! have psql; then
  say "Installing postgresql-client + redis-tools..."
  sudo apt-get install -y -qq postgresql-client redis-tools
else
  say "psql already installed."
fi

# ── 6. Repo clones (skip if exist) ────────────────────────────────────
REPOS_DIR="$HOME"
clone_if_missing() {
  local repo="$1" dir="$2"
  if [[ -d "$REPOS_DIR/$dir" ]]; then
    say "$dir already cloned at ~/$dir"
  else
    say "Cloning $repo to ~/$dir ..."
    git clone "$repo" "$REPOS_DIR/$dir"
  fi
}
clone_if_missing "https://github.com/sawancse/safar-platform.git" "safar-platform"
clone_if_missing "https://github.com/sawancse/safar-web.git"      "safar-web"

# ── 7. Optional: VS Code (one popular IDE) ────────────────────────────
if ! have code; then
  read -rp "Install VS Code? (y/N): " yn
  if [[ "${yn,,}" == "y" ]]; then
    say "Installing VS Code..."
    sudo apt-get install -y -qq wget
    wget -qO- https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor > /tmp/microsoft.gpg
    sudo install -o root -g root -m 644 /tmp/microsoft.gpg /etc/apt/trusted.gpg.d/
    echo "deb [arch=amd64,arm64,armhf signed-by=/etc/apt/trusted.gpg.d/microsoft.gpg] https://packages.microsoft.com/repos/code stable main" | sudo tee /etc/apt/sources.list.d/vscode.list > /dev/null
    sudo apt-get update -qq
    sudo apt-get install -y -qq code
  fi
fi

# ── Done ──────────────────────────────────────────────────────────────
echo
say "✅ Setup complete."
echo
echo "Next steps:"
echo "  1. Log out + log back in (so docker group membership takes effect)"
echo "  2. cd ~/safar-platform"
echo "  3. docker compose up -d           # postgres + redis + kafka"
echo "  4. mvn -pl services/chef-service spring-boot:run"
echo "  5. cd ~/safar-web && npm install && npm run dev"
echo
echo "Service ports for reference:"
echo "  api-gateway      8080      auth         8888"
echo "  user-service     8092      listing      8083"
echo "  search           8084      booking      8095"
echo "  payment          8086      review       8087"
echo "  media            8088      notification 8089"
echo "  ai-service       8090      messaging    8091"
echo "  chef-service     8093      flight       8094"
echo "  supply-service   8096"
echo "  safar-web (Next) 3000      admin (Vite) 3001"
