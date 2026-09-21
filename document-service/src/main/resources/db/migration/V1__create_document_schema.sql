CREATE TABLE IF NOT EXISTS loan_documents
(
    id                   VARCHAR(36)  NOT NULL,
    loan_application_id  VARCHAR(36)  NOT NULL,
    user_id              VARCHAR(36)  NOT NULL,
    document_type        VARCHAR(50)  NOT NULL,
    bucket_name          VARCHAR(100),
    object_key           VARCHAR(500),
    original_file_name   VARCHAR(255),
    mime_type            VARCHAR(100),
    file_size_bytes      BIGINT,
    extracted_text       TEXT,
    extracted_income     VARCHAR(50),
    income_verified      TINYINT(1)   DEFAULT 0,
    verified             TINYINT(1)   NOT NULL DEFAULT 0,
    verification_note    TEXT,
    verified_at          DATETIME,
    verified_by          VARCHAR(36),
    rejected             TINYINT(1)   NOT NULL DEFAULT 0,
    rejection_reason     TEXT,
    correlation_id       VARCHAR(50),
    uploaded_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_loan_documents PRIMARY KEY (id),
    INDEX idx_doc_loan_id  (loan_application_id),
    INDEX idx_doc_user_id  (user_id),
    INDEX idx_doc_type     (document_type),
    INDEX idx_doc_verified (verified)

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;