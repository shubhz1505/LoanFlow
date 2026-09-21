package com.loanflow.credit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanflow.commons.enums.RiskTier;
import com.loanflow.commons.events.LoanApplicationSubmittedEvent;
import com.loanflow.commons.events.LoanCreditScoredEvent;
import com.loanflow.commons.utils.EmiCalculator;
import com.loanflow.credit.client.MlEngineClient;
import com.loanflow.credit.entity.CreditAssessment;
import com.loanflow.credit.repository.CreditAssessmentRepository;
import com.loanflow.commons.kafka.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditScoringService {

    private final MlEngineClient             mlEngineClient;
    private final CreditAssessmentRepository assessmentRepository;
    private final EventPublisher             eventPublisher;
    private final ObjectMapper               objectMapper;

    private static final String TOPIC_CREDIT_SCORED = "loan.credit.scored";

    @Transactional
    public void scoreApplication(LoanApplicationSubmittedEvent event) {
        log.info("[{}] Scoring loan {}", event.getCorrelationId(),
                event.getLoanApplicationId());

        BigDecimal existingEmi = event.getExistingEmiAmount() != null
                ? event.getExistingEmiAmount() : BigDecimal.ZERO;

        BigDecimal dtiRatio = EmiCalculator.debtToIncomeRatio(
                existingEmi, event.getMonthlyIncome());

        BigDecimal projectedEmi = EmiCalculator.calculateEmi(
                event.getRequestedAmount(),
                BigDecimal.valueOf(12.0),
                event.getTenureMonths());

        BigDecimal creditUtilization = EmiCalculator.debtToIncomeRatio(
                existingEmi.add(projectedEmi), event.getMonthlyIncome());

        MlEngineClient.MlScoreRequest mlRequest =
                MlEngineClient.MlScoreRequest.builder()
                        .loanApplicationId(event.getLoanApplicationId())
                        .monthlyIncome(event.getMonthlyIncome().doubleValue())
                        .requestedAmount(event.getRequestedAmount().doubleValue())
                        .tenureMonths(event.getTenureMonths())
                        .existingEmiAmount(existingEmi.doubleValue())
                        .existingLoanCount(event.getExistingLoanCount() != null
                                ? event.getExistingLoanCount() : 0)
                        .employmentType(event.getEmploymentType().name())
                        .debtToIncomeRatio(dtiRatio.doubleValue())
                        .creditUtilization(creditUtilization.doubleValue())
                        .correlationId(event.getCorrelationId())
                        .build();

        MlEngineClient.MlScoreResponse mlResponse =
                mlEngineClient.score(mlRequest);

        boolean isManualReview =
                "MANUAL_REVIEW".equals(mlResponse.getRiskTier());
        RiskTier riskTier = isManualReview
                ? RiskTier.MANUAL_REVIEW
                : RiskTier.valueOf(mlResponse.getRiskTier());

        CreditAssessment assessment = CreditAssessment.builder()
                .loanApplicationId(event.getLoanApplicationId())
                .userId(event.getUserId())
                .monthlyIncome(event.getMonthlyIncome())
                .requestedAmount(event.getRequestedAmount())
                .tenureMonths(event.getTenureMonths())
                .existingEmiAmount(existingEmi)
                .existingLoanCount(event.getExistingLoanCount())
                .employmentType(event.getEmploymentType())
                .debtToIncomeRatio(dtiRatio)
                .creditScore(mlResponse.getCreditScore())
                .riskTier(riskTier)
                .maxEligibleAmount(BigDecimal.valueOf(
                                mlResponse.getMaxEligibleAmount())
                        .setScale(2, RoundingMode.HALF_UP))
                .recommendedInterestRate(BigDecimal.valueOf(
                                mlResponse.getRecommendedInterestRate())
                        .setScale(3, RoundingMode.HALF_UP))
                .rejectionReasons(toJson(mlResponse.getRejectionReasons()))
                .shapExplanation(toJson(mlResponse.getShapExplanation()))
                .isManualReview(isManualReview)
                .mlEngineVersion(mlResponse.getModelVersion())
                .scoringDurationMs(mlResponse.getScoringDurationMs())
                .correlationId(event.getCorrelationId())
                .build();

        assessmentRepository.save(assessment);
        log.info("[{}] Credit assessment saved: score={} tier={}",
                event.getCorrelationId(),
                mlResponse.getCreditScore(), riskTier);

        LoanCreditScoredEvent scoredEvent = LoanCreditScoredEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(event.getCorrelationId())
                .occurredAt(LocalDateTime.now())
                .sourceService("credit-service")
                .eventVersion("1.0")
                .loanApplicationId(event.getLoanApplicationId())
                .userId(event.getUserId())
                .creditScore(mlResponse.getCreditScore())
                .riskTier(riskTier)
                .maxEligibleAmount(assessment.getMaxEligibleAmount())
                .recommendedInterestRate(assessment.getRecommendedInterestRate())
                .rejectionReasons(mlResponse.getRejectionReasons() != null
                        ? mlResponse.getRejectionReasons() : List.of())
                .isManualReview(isManualReview)
                .scoredAt(LocalDateTime.now())
                .build();

        eventPublisher.publish(TOPIC_CREDIT_SCORED,
                event.getLoanApplicationId(),
                scoredEvent,
                event.getCorrelationId());

        log.info("[{}] Published loan.credit.scored for loan {}",
                event.getCorrelationId(), event.getLoanApplicationId());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}