CREATE TABLE IF NOT EXISTS kyc_audit_log
(
    id                VARCHAR(36)  NOT NULL,
    user_id           VARCHAR(36)  NOT NULL,
    from_status       VARCHAR(30)  NOT NULL,
    to_status         VARCHAR(30)  NOT NULL,
    performed_by      VARCHAR(36),
    performed_by_role VARCHAR(30),
    remarks           TEXT,
    ip_address        VARCHAR(45),
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_kyc_audit_log PRIMARY KEY (id),
    INDEX idx_kyc_log_user_id (user_id),

    CONSTRAINT fk_kyc_log_user FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE CASCADE

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;