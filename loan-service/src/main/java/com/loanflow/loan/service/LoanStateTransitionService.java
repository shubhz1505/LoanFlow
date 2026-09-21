package com.loanflow.loan.service;

import com.loanflow.commons.enums.LoanStatus;
import com.loanflow.loan.entity.LoanApplication;
import com.loanflow.loan.entity.LoanStateHistory;
import com.loanflow.loan.repository.LoanApplicationRepository;
import com.loanflow.loan.repository.LoanStateHistoryRepository;
import com.loanflow.loan.statemachine.LoanStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanStateTransitionService {

    private final LoanApplicationRepository  loanRepository;
    private final LoanStateHistoryRepository historyRepository;

    @Transactional
    public void transitionTo(LoanApplication loan,
                             LoanStatus newStatus,
                             String performedBy,
                             String performedByRole,
                             String remarks) {

        LoanStatus oldStatus = loan.getStatus();
        LoanStateMachine.validateTransition(oldStatus, newStatus);

        log.info("Loan {} transitioning {} → {} by {}",
                loan.getId(), oldStatus, newStatus, performedBy);

        historyRepository.save(LoanStateHistory.builder()
                .loanApplicationId(loan.getId())
                .fromStatus(oldStatus)
                .toStatus(newStatus)
                .performedBy(performedBy)
                .performedByRole(performedByRole)
                .remarks(remarks)
                .correlationId(loan.getCorrelationId())
                .build());

        loan.setStatus(newStatus);
        switch (newStatus) {
            case SUBMITTED            -> loan.setSubmittedAt(LocalDateTime.now());
            case APPROVED             -> loan.setApprovedAt(LocalDateTime.now());
            case REJECTED             -> loan.setRejectedAt(LocalDateTime.now());
            case DISBURSED            -> loan.setDisbursedAt(LocalDateTime.now());
            case CLOSED               -> loan.setClosedAt(LocalDateTime.now());
            default                   -> {}
        }

        loanRepository.save(loan);
    }
}