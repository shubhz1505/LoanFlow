-- V1__create_users_table.sql
-- Flyway runs this ONCE on first startup.
-- Never edit this file after running — create V2, V3 etc for changes.

CREATE TABLE IF NOT EXISTS users
(
    -- Primary key: UUID string, not auto-increment integer.
    -- Why UUID not BIGINT?
    -- 1. No sequential ID leak: attacker can't guess userId+1
    -- 2. Works across distributed systems without coordination
    -- 3. Can generate ID in application before DB insert
    -- Trade-off: larger storage, slightly slower index
    id                     VARCHAR(36)  NOT NULL,

    full_name              VARCHAR(100) NOT NULL,

    -- Unique index below — enforced at DB level, not just app level
    -- App-level unique check has TOCTOU race condition:
    --   Thread 1: check email → not exists
    --   Thread 2: check email → not exists  
    --   Thread 1: insert → success
    --   Thread 2: insert → duplicate (DB catches it)
    email                  VARCHAR(150) NOT NULL,
    password               VARCHAR(255) NOT NULL, -- BCrypt hash, always 60 chars

    phone                  VARCHAR(15)  NOT NULL,
    date_of_birth          DATE,
    address                TEXT         NOT NULL,
    city                   VARCHAR(100),
    state                  VARCHAR(100),
    pincode                VARCHAR(10),

    -- Sensitive document references — NOT the actual numbers
    -- We store: "XXXX-XXXX-1234" for Aadhaar
    -- Actual numbers encrypted and stored in Vault
    -- If this DB is compromised: attacker gets masked references only
    pan_reference          VARCHAR(20),
    aadhaar_reference      VARCHAR(20),

    role                   VARCHAR(30)  NOT NULL DEFAULT 'APPLICANT',
    kyc_status             VARCHAR(30)  NOT NULL DEFAULT 'NOT_INITIATED',

    -- Account state flags
    enabled                BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_locked     BOOLEAN      NOT NULL DEFAULT TRUE,

    -- KYC tracking
    kyc_submitted_at       DATETIME,
    kyc_verified_at        DATETIME,
    kyc_rejection_reason   TEXT,
    kyc_attempt_count      INT          NOT NULL DEFAULT 0,

    -- Login security tracking
    last_login_at          DATETIME,
    failed_login_attempts  INT          NOT NULL DEFAULT 0,
    locked_until           DATETIME,

    -- Audit timestamps
    -- DEFAULT CURRENT_TIMESTAMP: DB sets this, not application
    -- ON UPDATE CURRENT_TIMESTAMP: DB updates this automatically
    -- Why DB not application? Clock skew between app servers.
    -- Two app pods might have slightly different clocks.
    -- DB clock is single source of truth.
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_phone UNIQUE (phone),

    -- Indexes on columns used in WHERE clauses
    -- Without index: full table scan on every login attempt
    -- With index: O(log n) lookup
    INDEX idx_users_email      (email),
    INDEX idx_users_kyc_status (kyc_status),
    INDEX idx_users_role       (role),
    INDEX idx_users_created_at (created_at)

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4        -- utf8mb4 supports full Unicode + emojis
    COLLATE = utf8mb4_unicode_ci;    -- Case-insensitive comparison for strings