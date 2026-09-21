-- V2__create_refresh_tokens_table.sql

CREATE TABLE IF NOT EXISTS refresh_tokens
(
    id           VARCHAR(36)  NOT NULL,

    -- The token itself: UUID v4 string
    -- Long enough to be unguessable (122 bits of entropy)
    token        VARCHAR(255) NOT NULL,

    user_id      VARCHAR(36)  NOT NULL,
    expires_at   DATETIME     NOT NULL,

    -- When revoked=true: token cannot be used
    -- We keep the row (don't delete) for audit purposes
    -- "Was this token used after revocation? Yes → attack attempt"
    revoked      BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Reuse detection: if a revoked token is presented again,
    -- we know the original token was stolen.
    -- We can then revoke ALL tokens for this user.
    revoked_at   DATETIME,

    -- Device fingerprinting — helps users see "active sessions"
    -- "Logged in from Chrome on Windows" etc.
    device_info  TEXT,
    ip_address   VARCHAR(45), -- IPv6 max length is 45 chars

    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),

    -- Lookup by token value — happens on every refresh request
    CONSTRAINT uq_refresh_token UNIQUE (token),

    -- Lookup all tokens for a user — happens on logout + reuse detection
    INDEX idx_rt_user_id   (user_id),
    INDEX idx_rt_token     (token),
    INDEX idx_rt_expires_at (expires_at),  -- Cleanup scheduler uses this

    CONSTRAINT fk_rt_user FOREIGN KEY (user_id)
    REFERENCES users (id)
    ON DELETE CASCADE  -- User deleted → all their tokens deleted

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;