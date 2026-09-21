package com.loanflow.commons.events;

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
public class PaymentReceivedEvent extends BaseEvent {

    private String        paymentId;
    private String        loanApplicationId;
    private String        userId;
    private String        userEmail;
    private BigDecimal    amountPaid;
    private BigDecimal    principalComponent;
    private BigDecimal    interestComponent;
    private BigDecimal    outstandingBalance;
    private Integer       emiNumber;
    private String        paymentMode;
    private String        transactionReference;
    private LocalDateTime paidAt;
}