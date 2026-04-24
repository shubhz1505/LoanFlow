package com.loanflow.commons.events;

import com.loanflow.commons.enums.EmloymentType;
import com.loanflow.commons.enums.LoanType;
import jdk.jfr.DataAmount;
import lombok.AllArgeConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoargsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualAndHashCode (callSuper = true)

public class LoanApplicationSubmittedEvent extends BaseEvent {
    private String loanApplicationId;
    private String userId;
    private String userEmail;
    private String userfullName;

    private LoanType loanType;
    private Integer tenureMonths;
    private BigDecimal monthlyIncome;
    private EmploymentType employmentType;

    private BigDecimal existingEmiAmount;
    private Integer existingLoanCount;
    private String loanPurpose;

    private LocalDateTime submittedAt;
    private String applicantIpAddress;
}
