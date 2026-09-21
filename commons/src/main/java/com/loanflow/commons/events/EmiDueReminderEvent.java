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
public class EmiDueReminderEvent extends BaseEvent {

    private String        loanApplicationId;
    private String        userId;
    private String        userEmail;
    private String        userPhone;
    private BigDecimal    emiAmount;
    private LocalDate     dueDate;
    private Integer       daysUntilDue;
    private Integer       emiNumber;
    private BigDecimal    outstandingBalance;
    private LocalDateTime triggeredAt;
}