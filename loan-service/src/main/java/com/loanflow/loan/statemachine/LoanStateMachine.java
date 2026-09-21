package com.loanflow.loan.statemachine;

import com.loanflow.commons.enums.LoanStatus;
import com.loanflow.commons.exceptions.InvalidStateException;

import java.util.Map;
import java.util.Set;

public class LoanStateMachine {

    private static final Map<LoanStatus, Set<LoanStatus>> VALID_TRANSITIONS =
            Map.ofEntries(
                    Map.entry(LoanStatus.DRAFT,
                            Set.of(LoanStatus.SUBMITTED)),
                    Map.entry(LoanStatus.SUBMITTED,
                            Set.of(LoanStatus.DOCUMENT_PENDING,
                                    LoanStatus.CREDIT_CHECK_IN_PROGRESS)),
                    Map.entry(LoanStatus.DOCUMENT_PENDING,
                            Set.of(LoanStatus.CREDIT_CHECK_IN_PROGRESS,
                                    LoanStatus.REJECTED)),
                    Map.entry(LoanStatus.CREDIT_CHECK_IN_PROGRESS,
                            Set.of(LoanStatus.UNDER_REVIEW,
                                    LoanStatus.CONDITIONALLY_APPROVED,
                                    LoanStatus.REJECTED)),
                    Map.entry(LoanStatus.UNDER_REVIEW,
                            Set.of(LoanStatus.CONDITIONALLY_APPROVED,
                                    LoanStatus.APPROVED,
                                    LoanStatus.REJECTED)),
                    Map.entry(LoanStatus.CONDITIONALLY_APPROVED,
                            Set.of(LoanStatus.APPROVED,
                                    LoanStatus.REJECTED,
                                    LoanStatus.DOCUMENT_PENDING)),
                    Map.entry(LoanStatus.APPROVED,
                            Set.of(LoanStatus.DISBURSEMENT_PENDING)),
                    Map.entry(LoanStatus.DISBURSEMENT_PENDING,
                            Set.of(LoanStatus.DISBURSED, LoanStatus.REJECTED)),
                    Map.entry(LoanStatus.DISBURSED,
                            Set.of(LoanStatus.ACTIVE)),
                    Map.entry(LoanStatus.ACTIVE,
                            Set.of(LoanStatus.CLOSED, LoanStatus.NPA)),
                    Map.entry(LoanStatus.NPA,
                            Set.of(LoanStatus.ACTIVE,
                                    LoanStatus.WRITTEN_OFF,
                                    LoanStatus.CLOSED)),
                    Map.entry(LoanStatus.REJECTED,   Set.of()),
                    Map.entry(LoanStatus.CLOSED,     Set.of()),
                    Map.entry(LoanStatus.WRITTEN_OFF, Set.of())
            );

    private LoanStateMachine() {}

    public static void validateTransition(LoanStatus from, LoanStatus to) {
        Set<LoanStatus> allowed = VALID_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new InvalidStateException(
                    "Invalid transition: " + from + " → " + to +
                            ". Allowed: " + allowed);
        }
    }

    public static boolean canTransition(LoanStatus from, LoanStatus to) {
        return VALID_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static boolean isTerminal(LoanStatus status) {
        return status == LoanStatus.REJECTED
                || status == LoanStatus.CLOSED
                || status == LoanStatus.WRITTEN_OFF;
    }
}