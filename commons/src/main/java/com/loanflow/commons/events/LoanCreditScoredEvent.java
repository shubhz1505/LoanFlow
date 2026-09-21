package com.loanflow.commons.events;

import com.loanflow.commons.enums.RiskTier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class LoanCreditScoredEvent extends BaseEvent {

    private String       loanApplicationId;
    private String       userId;
    private Integer      creditScore;
    private RiskTier     riskTier;
    private BigDecimal   maxEligibleAmount;
    private BigDecimal   recommendedInterestRate;
    private List<String> rejectionReasons;
    private Boolean      isManualReview;
    private LocalDateTime scoredAt;
}