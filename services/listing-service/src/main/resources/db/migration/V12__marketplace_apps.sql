CREATE TABLE listings.marketplace_apps (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    developer_id    UUID NOT NULL,
    app_name        VARCHAR(100) NOT NULL,
    description     TEXT,
    client_id       VARCHAR(64) NOT NULL UNIQUE,
    client_secret   VARCHAR(256) NOT NULL,
    redirect_uris   TEXT NOT NULL DEFAULT '',
    scopes          TEXT NOT NULL DEFAULT '',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    webhook_url     VARCHAR(500),
    webhook_secret  VARCHAR(64),
    rate_limit_rpm  INT NOT NULL DEFAULT 60,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE listings.app_installations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_id          UUID NOT NULL,
    host_id         UUID NOT NULL,
    scopes_granted  TEXT NOT NULL DEFAULT '',
    access_token    VARCHAR(256),
    refresh_token   VARCHAR(256),
    expires_at      TIMESTAMPTZ,
    installed_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(app_id, host_id)
);

CREATE TABLE listings.webhook_deliveries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_id          UUID NOT NULL,
    event_type      VARCHAR(50) NOT NULL,
    payload         TEXT NOT NULL,
    response_status INT,
    delivered_at    TIMESTAMPTZ,
    attempt_count   INT NOT NULL DEFAULT 0,
    next_retry_at   TIMESTAMPTZ,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);
