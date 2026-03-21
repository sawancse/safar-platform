CREATE TABLE users.nomad_posts (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id  UUID NOT NULL,
    city       VARCHAR(100),
    category   VARCHAR(50) NOT NULL,
    title      VARCHAR(200) NOT NULL,
    body       TEXT NOT NULL,
    tags       TEXT NOT NULL DEFAULT '',
    upvotes    INT NOT NULL DEFAULT 0,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    pinned     BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users.nomad_post_comments (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id    UUID NOT NULL,
    author_id  UUID NOT NULL,
    body       TEXT NOT NULL,
    upvotes    INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users.nomad_connections (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id UUID NOT NULL,
    recipient_id UUID NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(requester_id, recipient_id)
);

CREATE TABLE users.skill_swaps (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poster_id  UUID NOT NULL,
    offering   VARCHAR(200) NOT NULL,
    seeking    VARCHAR(200) NOT NULL,
    city       VARCHAR(100),
    status     VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
