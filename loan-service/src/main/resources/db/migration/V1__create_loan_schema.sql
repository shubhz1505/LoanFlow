CREATE TABLE IF NOT EXISTS loan_applications
(
    id                         VARCHAR(36)    NOT NULL,
    user_id                    VARCHAR(36)    NOT NULL,
    user_email                 VARCHAR(150)   NOT NULL,
    user_full_name             VARCHAR(100)   NOT NULL,
    loan_type                  VARCHAR(30)    NOT NULL,
    requested_amount           DECIMAL(15,2)  NOT NULL,
    tenure_months              INT            NOT NULL,
    monthly_income             DECIMAL(15,2)  NOT NULL,
    employment_type            VARCHAR(30)    NOT NULL,
    existing_emi_amount        DECIMAL(15,2)  DEFAULT 0.00,
    existing_loan_count        INT            DEFAULT 0,
    loan_purpose               TEXT,

    credit_score               INT,
    risk_tier                  VARCHAR(20),
    max_eligible_amount        DECIMAL(15,2),
    recommended_interest_rate  DECIMAL(6,3),
    credit_manual_review       TINYINT(1)     DEFAULT 0,

    maker_officer_id           VARCHAR(36),
    maker_decision             VARCHAR(30),
    maker_remarks              TEXT,
    maker_decision_at          DATETIME,
    checker_officer_id         VARCHAR(36),
    checker_decision_at        DATETIME,
    checker_remarks            TEXT,

    approved_amount            DECIMAL(15,2),
    approved_interest_rate     DECIMAL(6,3),
    approved_tenure_months     INT,
    emi_amount                 DECIMAL(15,2),
    disbursement_date          DATE,
    first_emi_date             DATE,

    status                     VARCHAR(30)    NOT NULL DEFAULT 'DRAFT',
    rejection_reasons          TEXT,
    submitted_at               DATETIME,
    approved_at                DATETIME,
    rejected_at                DATETIME,
    disbursed_at               DATETIME,
    closed_at                  DATETIME,
    correlation_id             VARCHAR(50),

    created_at                 DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_loan_applications PRIMARY KEY (id),
    INDEX idx_loan_user_id     (user_id),
    INDEX idx_loan_status      (status),
    INDEX idx_loan_type        (loan_type),
    INDEX idx_loan_created_at  (created_at),
    INDEX idx_loan_maker       (maker_officer_id)

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS loan_state_history
(
    id                   VARCHAR(36)  NOT NULL,
    loan_application_id  VARCHAR(36)  NOT NULL,
    from_status          VARCHAR(30)  NOT NULL,
    to_status            VARCHAR(30)  NOT NULL,
    performed_by         VARCHAR(36),
    performed_by_role    VARCHAR(30),
    remarks              TEXT,
    correlation_id       VARCHAR(50),
    transitioned_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_loan_state_history PRIMARY KEY (id),
    INDEX idx_lsh_loan_id (loan_application_id),

    CONSTRAINT fk_lsh_loan FOREIGN KEY (loan_application_id)
    REFERENCES loan_applications (id) ON DELETE CASCADE

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;