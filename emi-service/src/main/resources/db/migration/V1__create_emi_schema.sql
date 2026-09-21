CREATE TABLE IF NOT EXISTS loan_accounts
(
    loan_application_id     VARCHAR(36)   NOT NULL,
    user_id                 VARCHAR(36)   NOT NULL,
    principal_amount        DECIMAL(15,2) NOT NULL,
    outstanding_principal   DECIMAL(15,2) NOT NULL,
    total_interest_payable  DECIMAL(15,2) NOT NULL,
    total_interest_paid     DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_principal_paid    DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_penalty_charged   DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    annual_interest_rate    DECIMAL(6,3)  NOT NULL,
    tenure_months           INT           NOT NULL,
    emis_paid               INT           NOT NULL DEFAULT 0,
    emis_remaining          INT           NOT NULL,
    emi_amount              DECIMAL(15,2) NOT NULL,
    first_emi_date          DATE,
    last_emi_date           DATE,
    next_emi_due_date       DATE,
    account_status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    last_updated_at         DATETIME,

    CONSTRAINT pk_loan_accounts PRIMARY KEY (loan_application_id),
    INDEX idx_la_user_id   (user_id),
    INDEX idx_la_status    (account_status)

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS emi_schedule
(
    id                   VARCHAR(36)   NOT NULL,
    loan_application_id  VARCHAR(36)   NOT NULL,
    user_id              VARCHAR(36)   NOT NULL,
    emi_number           INT           NOT NULL,
    total_emis           INT           NOT NULL,
    emi_amount           DECIMAL(15,2) NOT NULL,
    principal_component  DECIMAL(15,2) NOT NULL,
    interest_component   DECIMAL(15,2) NOT NULL,
    outstanding_balance  DECIMAL(15,2) NOT NULL,
    due_date             DATE          NOT NULL,
    status               VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    paid_date            DATE,
    amount_paid          DECIMAL(15,2),
    penalty_amount       DECIMAL(15,2) DEFAULT 0.00,
    payment_id           VARCHAR(36),
    overdue_days         INT           DEFAULT 0,
    reminder_sent        TINYINT(1)    DEFAULT 0,
    reminder_sent_at     DATETIME,
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_emi_schedule PRIMARY KEY (id),
    CONSTRAINT uq_emi_loan_number UNIQUE (loan_application_id, emi_number),

    INDEX idx_emi_loan_id  (loan_application_id),
    INDEX idx_emi_due_date (due_date),
    INDEX idx_emi_status   (status),

    CONSTRAINT fk_emi_account FOREIGN KEY (loan_application_id)
    REFERENCES loan_accounts (loan_application_id)
    ON DELETE CASCADE

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;