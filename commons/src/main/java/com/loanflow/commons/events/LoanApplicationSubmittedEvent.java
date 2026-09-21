package com.loanflow.commons.events;

import com.loanflow.commons.enums.EmploymentType;
import com.loanflow.commons.enums.LoanType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class LoanApplicationSubmittedEvent extends BaseEvent {

    private String         loanApplicationId;
    private String         userId;
    private String         userEmail;
    private String         userFullName;
    private LoanType       loanType;
    private BigDecimal     requestedAmount;
    private Integer        tenureMonths;
    private BigDecimal     monthlyIncome;
    private EmploymentType employmentType;
    private BigDecimal     existingEmiAmount;
    private Integer        existingLoanCount;
    private String         loanPurpose;
    private LocalDateTime  submittedAt;
    private String         applicantIpAddress;
}