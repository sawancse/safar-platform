# Safar Platform — Ubuntu Deployment Guide

## 1. Server Setup

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install essentials
sudo apt install -y curl wget git unzip software-properties-common apt-transport-https ca-certificates gnupg lsb-release

# Java 17
sudo apt install -y openjdk-17-jdk
java -version

# Maven
sudo apt install -y maven

# Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
node -v && npm -v

# Python 3.11
sudo apt install -y python3.11 python3.11-venv python3-pip

# Docker & Docker Compose
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo usermod -aG docker $USER
```

## 2. Clone & Configure

```bash
cd /opt
sudo mkdir safar && sudo chown $USER:$USER safar
git clone <your-repo-url> /opt/safar/safar-platform
cd /opt/safar/safar-platform

# Create .env from template
cp .env.example .env
nano .env   # Fill in production values
```

## 3. Start Infrastructure (Docker)

```bash
cd /opt/safar/safar-platform

# Start PostgreSQL, Redis, Kafka, Zookeeper, Elasticsearch
docker compose up -d postgres redis zookeeper kafka elasticsearch

# Verify all healthy
docker compose ps
docker compose logs -f postgres --tail=20   # Wait for "ready to accept connections"
docker compose logs -f elasticsearch --tail=20   # Wait for "started"
```

## 4. Create Database Schemas

```bash
docker exec -it $(docker ps -qf "name=postgres") psql -U postgres -d safar -c "
  CREATE SCHEMA IF NOT EXISTS auth;
  CREATE SCHEMA IF NOT EXISTS users;
  CREATE SCHEMA IF NOT EXISTS listings;
  CREATE SCHEMA IF NOT EXISTS bookings;
  CREATE SCHEMA IF NOT EXISTS payments;
  CREATE SCHEMA IF NOT EXISTS reviews;
  CREATE SCHEMA IF NOT EXISTS notifications;
  CREATE SCHEMA IF NOT EXISTS messaging;
"
```

## 5. Build All Java Services

```bash
cd /opt/safar/safar-platform
mvn clean package -DskipTests -q
```

## 6. Start Java Services

Create a systemd service template — `/etc/systemd/system/safar@.service`:

```bash
sudo tee /etc/systemd/system/safar@.service > /dev/null << 'EOF'
[Unit]
Description=Safar %i Service
After=network.target docker.service
Requires=docker.service

[Service]
User=safar
WorkingDirectory=/opt/safar/safar-platform/services/%i
EnvironmentFile=/opt/safar/safar-platform/.env
ExecStart=/usr/bin/java \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -Xms64m -Xmx384m \
  -jar target/%i-1.0.0-SNAPSHOT.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
```

Start all services in order:

```bash
# Create safar user
sudo useradd -r -s /bin/false safar
sudo chown -R safar:safar /opt/safar

# Start in dependency order
sudo systemctl enable --now safar@api-gateway
sudo systemctl enable --now safar@auth-service
sudo systemctl enable --now safar@user-service
sudo systemctl enable --now safar@listing-service
sudo systemctl enable --now safar@search-service
sudo systemctl enable --now safar@booking-service
sudo systemctl enable --now safar@payment-service
sudo systemctl enable --now safar@review-service
sudo systemctl enable --now safar@media-service
sudo systemctl enable --now safar@notification-service
sudo systemctl enable --now safar@messaging-service

# Check status
sudo systemctl status safar@*
```

## 7. AI Service (Python)

```bash
cd /opt/safar/safar-platform/services/ai-service
python3.11 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# Create systemd service
sudo tee /etc/systemd/system/safar-ai.service > /dev/null << 'EOF'
[Unit]
Description=Safar AI Service
After=network.target

[Service]
User=safar
WorkingDirectory=/opt/safar/safar-platform/services/ai-service
EnvironmentFile=/opt/safar/safar-platform/.env
ExecStart=/opt/safar/safar-platform/services/ai-service/venv/bin/uvicorn main:app --host 0.0.0.0 --port 8090 --workers 2
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now safar-ai
```

## 8. Frontend Web (Next.js)

```bash
cd /opt/safar/safar-platform/frontend/web
npm ci
NEXT_PUBLIC_API_URL=https://api.yourdomain.com npm run build

# Systemd service
sudo tee /etc/systemd/system/safar-web.service > /dev/null << 'EOF'
[Unit]
Description=Safar Web Frontend
After=network.target

[Service]
User=safar
WorkingDirectory=/opt/safar/safar-platform/frontend/web
Environment=NODE_ENV=production
Environment=PORT=3000
ExecStart=/usr/bin/node .next/standalone/server.js
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now safar-web
```

## 9. Admin Dashboard

```bash
cd /opt/safar/safar-platform/admin
npm ci
VITE_API_URL=https://api.yourdomain.com npm run build
# dist/ folder is static — serve via nginx
```

## 10. Nginx Reverse Proxy

```bash
sudo apt install -y nginx certbot python3-certbot-nginx

sudo tee /etc/nginx/sites-available/safar > /dev/null << 'EOF'
# API Gateway
server {
    listen 80;
    server_name api.yourdomain.com;
    client_max_body_size 50M;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket support (for messaging)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}

# Web Frontend
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# Admin Dashboard
server {
    listen 80;
    server_name admin.yourdomain.com;

    root /opt/safar/safar-platform/admin/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
EOF

sudo ln -s /etc/nginx/sites-available/safar /etc/nginx/sites-enabled/
sudo rm /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx

# SSL certificates
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com -d api.yourdomain.com -d admin.yourdomain.com
```

## 11. Verify Everything

```bash
# Infrastructure
docker compose ps

# Java services
for svc in api-gateway auth-service user-service listing-service search-service booking-service payment-service review-service media-service notification-service messaging-service; do
  echo -n "$svc: "
  sudo systemctl is-active safar@$svc
done

# AI service
sudo systemctl status safar-ai

# Web frontend
sudo systemctl status safar-web

# Health check via API Gateway
curl -s http://localhost:8080/actuator/health | python3 -m json.tool

# Nginx
sudo nginx -t
```

## 12. Useful Operations

```bash
# View logs for a service
sudo journalctl -u safar@listing-service -f --lines=100

# Restart a single service
sudo systemctl restart safar@booking-service

# Rebuild & redeploy one service
cd /opt/safar/safar-platform
mvn package -pl services/listing-service -DskipTests -q
sudo systemctl restart safar@listing-service

# Database backup
docker exec $(docker ps -qf "name=postgres") pg_dump -U postgres safar | gzip > /opt/safar/backups/safar_$(date +%Y%m%d).sql.gz

# Elasticsearch reindex (happens on startup, but manual)
curl -X DELETE http://localhost:9200/listings
sudo systemctl restart safar@search-service
```

## Notes

- Replace `yourdomain.com` with your actual domain
- Fill in all production secrets in `.env`
- Ensure S3 bucket CORS is configured for your frontend origin
- Set `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` for media-service
- Flyway migrations run automatically on service startup
