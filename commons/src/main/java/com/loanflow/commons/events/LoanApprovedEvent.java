package com.loanflow.commons.events;

import com.loanflow.commons.enums.LoanType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class LoanApprovedEvent extends BaseEvent {

    private String     loanApplicationId;
    private String     userId;
    private String     userEmail;
    private LoanType   loanType;
    private BigDecimal approvedAmount;
    private BigDecimal interestRate;
    private Integer    tenureMonths;
    private BigDecimal emiAmount;
    private LocalDate  disbursementDate;
    private String     approvedByOfficerId;
    private String     checkedByOfficerId;
    private LocalDateTime approvedAt;
}