package com.loanflow.commons.events;

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
public class PaymentOverdueEvent extends BaseEvent {

    private String        loanApplicationId;
    private String        userId;
    private String        userEmail;
    private Integer       overdueDays;
    private BigDecimal    overdueAmount;
    private BigDecimal    penaltyAmount;
    private Integer       emiNumber;
    private LocalDate     dueDate;
    private LocalDateTime detectedAt;
}