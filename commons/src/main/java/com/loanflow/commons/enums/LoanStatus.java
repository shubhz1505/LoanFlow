package com.loanflow.commons.enums;

public enum LoanStatus {
    DRAFT,
    SUBMITTED,

    DOCUMENT_PENDING,
    CREDIT_CHECK_IN_PROGRESS,
    UNDER_REVIEW,

    CONDITIONALLY_APPROVED,
    APPROVED,
    REJECTED,

    DISBURSEMENT_PENDING,
    DISBURSED,

    ACTIVE,
    CLOSED,

    NPA,
    WRITTEN_OFF
}
