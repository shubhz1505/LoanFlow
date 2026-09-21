package com.loanflow.loan.dto.response;

import com.loanflow.commons.enums.LoanStatus;
import com.loanflow.commons.enums.LoanType;
import com.loanflow.commons.enums.RiskTier;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LoanApplicationResponse {
    private String        id;
    private String        userId;
    private String        userFullName;
    private LoanType      loanType;
    private BigDecimal    requestedAmount;
    private Integer       tenureMonths;
    private BigDecimal    monthlyIncome;
    private LoanStatus    status;
    private Integer       creditScore;
    private RiskTier      riskTier;
    private BigDecimal    approvedAmount;
    private BigDecimal    approvedInterestRate;
    private BigDecimal    emiAmount;
    private LocalDate     disbursementDate;
    private String        makerDecision;
    private String        rejectionReasons;
    private List<StateHistoryResponse> stateHistory;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
}