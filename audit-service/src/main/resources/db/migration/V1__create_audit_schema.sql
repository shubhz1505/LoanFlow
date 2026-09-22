CREATE TABLE IF NOT EXISTS audit_logs
(
    id               VARCHAR(36)  NOT NULL,
    event_type       VARCHAR(50)  NOT NULL,
    entity_type      VARCHAR(50)  NOT NULL,
    entity_id        VARCHAR(36)  NOT NULL,
    actor_id         VARCHAR(36),
    actor_role       VARCHAR(30),
    actor_ip         VARCHAR(45),
    previous_state   TEXT,
    new_state        TEXT,
    metadata         TEXT,
    correlation_id   VARCHAR(50),
    source_service   VARCHAR(50),
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_audit_logs PRIMARY KEY (id),

    INDEX idx_al_entity_id   (entity_id),
    INDEX idx_al_actor_id    (actor_id),
    INDEX idx_al_event_type  (event_type),
    INDEX idx_al_created_at  (created_at),
    INDEX idx_al_correlation (correlation_id)

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;