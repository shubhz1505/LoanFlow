CREATE TABLE IF NOT EXISTS notifications
(
    id                   VARCHAR(36)  NOT NULL,
    user_id              VARCHAR(36)  NOT NULL,
    loan_application_id  VARCHAR(36),
    notification_type    VARCHAR(50)  NOT NULL,
    channel              VARCHAR(20)  NOT NULL DEFAULT 'EMAIL',
    recipient_address    VARCHAR(200) NOT NULL,
    subject              VARCHAR(500) NOT NULL,
    body                 TEXT         NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    failure_reason       TEXT,
    retry_count          INT          NOT NULL DEFAULT 0,
    sent_at              DATETIME,
    correlation_id       VARCHAR(50),
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_notifications PRIMARY KEY (id),

    INDEX idx_notif_user_id  (user_id),
    INDEX idx_notif_loan_id  (loan_application_id),
    INDEX idx_notif_type     (notification_type),
    INDEX idx_notif_status   (status)

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;