CREATE TABLE IF NOT EXISTS credit_assessments
(
    id                        VARCHAR(36)   NOT NULL,
    loan_application_id       VARCHAR(36)   NOT NULL,
    user_id                   VARCHAR(36)   NOT NULL,
    monthly_income            DECIMAL(15,2) NOT NULL,
    requested_amount          DECIMAL(15,2) NOT NULL,
    tenure_months             INT           NOT NULL,
    existing_emi_amount       DECIMAL(15,2) DEFAULT 0.00,
    existing_loan_count       INT           DEFAULT 0,
    employment_type           VARCHAR(30),
    debt_to_income_ratio      DECIMAL(6,3),
    credit_score              INT,
    risk_tier                 VARCHAR(20),
    max_eligible_amount       DECIMAL(15,2),
    recommended_interest_rate DECIMAL(6,3),
    rejection_reasons         TEXT,
    shap_explanation          TEXT,
    is_manual_review          TINYINT(1)    DEFAULT 0,
    ml_engine_version         VARCHAR(50),
    scoring_duration_ms       BIGINT,
    correlation_id            VARCHAR(50),
    scored_at                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_credit_assessments PRIMARY KEY (id),
    CONSTRAINT uq_credit_loan_id     UNIQUE (loan_application_id),

    INDEX idx_ca_loan_id   (loan_application_id),
    INDEX idx_ca_user_id   (user_id),
    INDEX idx_ca_risk_tier (risk_tier)

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;