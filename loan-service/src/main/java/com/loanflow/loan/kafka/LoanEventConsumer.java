package com.loanflow.loan.kafka;

import com.loanflow.commons.enums.LoanStatus;
import com.loanflow.commons.enums.RiskTier;
import com.loanflow.commons.events.DocumentVerifiedEvent;
import com.loanflow.commons.events.LoanCreditScoredEvent;
import com.loanflow.loan.entity.LoanApplication;
import com.loanflow.loan.repository.LoanApplicationRepository;
import com.loanflow.loan.service.LoanStateTransitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanEventConsumer {

    private final LoanApplicationRepository  loanRepository;
    private final LoanStateTransitionService transitionService;

    @KafkaListener(topics = "loan.credit.scored", groupId = "loan-service-group")
    @Transactional
    public void onCreditScored(@Payload LoanCreditScoredEvent event) {
        log.info("[{}] Credit scored for loan {} score={} tier={}",
                event.getCorrelationId(), event.getLoanApplicationId(),
                event.getCreditScore(), event.getRiskTier());

        LoanApplication loan = loanRepository
                .findById(event.getLoanApplicationId()).orElse(null);
        if (loan == null) return;

        loan.setCreditScore(event.getCreditScore());
        loan.setRiskTier(event.getRiskTier());
        loan.setMaxEligibleAmount(event.getMaxEligibleAmount());
        loan.setRecommendedInterestRate(event.getRecommendedInterestRate());
        loan.setCreditManualReview(event.getIsManualReview());

        if (event.getRiskTier() == RiskTier.VERY_HIGH) {
            transitionService.transitionTo(loan, LoanStatus.REJECTED,
                    "SYSTEM", "SYSTEM",
                    "Auto-rejected: risk tier VERY_HIGH. Score: "
                            + event.getCreditScore());
        } else {
            transitionService.transitionTo(loan, LoanStatus.UNDER_REVIEW,
                    "SYSTEM", "SYSTEM",
                    "Credit check complete. Score: " + event.getCreditScore()
                            + " | Risk: " + event.getRiskTier());
        }
    }

    @KafkaListener(topics = "document.verified", groupId = "loan-service-group")
    @Transactional
    public void onDocumentVerified(@Payload DocumentVerifiedEvent event) {
        log.info("[{}] Document verified for loan {} allComplete={}",
                event.getCorrelationId(), event.getLoanApplicationId(),
                event.getAllDocumentsComplete());

        if (!Boolean.TRUE.equals(event.getAllDocumentsComplete())) return;

        LoanApplication loan = loanRepository
                .findById(event.getLoanApplicationId()).orElse(null);
        if (loan == null || loan.getStatus() != LoanStatus.DOCUMENT_PENDING) return;

        transitionService.transitionTo(loan, LoanStatus.CREDIT_CHECK_IN_PROGRESS,
                "SYSTEM", "SYSTEM",
                "All documents verified. Initiating credit check.");
    }
}