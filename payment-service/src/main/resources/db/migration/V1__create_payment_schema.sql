CREATE TABLE IF NOT EXISTS payments
(
    id                        VARCHAR(36)   NOT NULL,
    loan_application_id       VARCHAR(36)   NOT NULL,
    user_id                   VARCHAR(36)   NOT NULL,
    user_email                VARCHAR(150)  NOT NULL,
    amount                    DECIMAL(15,2) NOT NULL,
    principal_component       DECIMAL(15,2) NOT NULL,
    interest_component        DECIMAL(15,2) NOT NULL,
    penalty_component         DECIMAL(15,2) DEFAULT 0.00,
    outstanding_balance_after DECIMAL(15,2) NOT NULL,
    emi_number                INT,
    payment_mode              VARCHAR(20)   NOT NULL,
    status                    VARCHAR(20)   NOT NULL DEFAULT 'SUCCESS',
    transaction_reference     VARCHAR(100)  UNIQUE,
    failure_reason            TEXT,
    correlation_id            VARCHAR(50),
    created_at                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at              DATETIME,

    CONSTRAINT pk_payments PRIMARY KEY (id),

    INDEX idx_pay_loan_id  (loan_application_id),
    INDEX idx_pay_user_id  (user_id),
    INDEX idx_pay_status   (status),
    INDEX idx_pay_txn_ref  (transaction_reference)

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS ledger_entries
(
    id                   VARCHAR(36)   NOT NULL,
    payment_id           VARCHAR(36)   NOT NULL,
    loan_application_id  VARCHAR(36)   NOT NULL,
    user_id              VARCHAR(36)   NOT NULL,
    entry_type           VARCHAR(10)   NOT NULL,
    account              VARCHAR(30)   NOT NULL,
    amount               DECIMAL(15,2) NOT NULL,
    running_balance      DECIMAL(15,2) NOT NULL,
    description          TEXT,
    correlation_id       VARCHAR(50),
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_ledger_entries PRIMARY KEY (id),

    INDEX idx_le_payment_id  (payment_id),
    INDEX idx_le_loan_id     (loan_application_id),
    INDEX idx_le_entry_type  (entry_type),
    INDEX idx_le_created_at  (created_at)

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;