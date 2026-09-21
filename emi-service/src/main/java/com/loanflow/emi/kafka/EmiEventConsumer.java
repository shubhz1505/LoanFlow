package com.loanflow.emi.kafka;

import com.loanflow.commons.events.LoanApprovedEvent;
import com.loanflow.commons.events.PaymentReceivedEvent;
import com.loanflow.emi.service.EmiScheduleGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmiEventConsumer {

    private final EmiScheduleGenerator scheduleGenerator;

    @KafkaListener(topics = "loan.approved", groupId = "emi-service-group")
    @Transactional
    public void onLoanApproved(@Payload LoanApprovedEvent event) {
        log.info("[{}] Received loan.approved for loan {} — generating schedule",
                event.getCorrelationId(), event.getLoanApplicationId());
        try {
            scheduleGenerator.generateSchedule(
                    event.getLoanApplicationId(),
                    event.getUserId(),
                    event.getApprovedAmount(),
                    event.getInterestRate(),
                    event.getTenureMonths(),
                    event.getEmiAmount(),
                    event.getDisbursementDate().plusDays(30));
        } catch (Exception ex) {
            log.error("[{}] Failed to generate schedule for loan {}: {}",
                    event.getCorrelationId(),
                    event.getLoanApplicationId(), ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = "payment.received", groupId = "emi-service-group")
    @Transactional
    public void onPaymentReceived(@Payload PaymentReceivedEvent event) {
        log.info("[{}] Received payment.received for loan {} EMI #{}",
                event.getCorrelationId(),
                event.getLoanApplicationId(), event.getEmiNumber());
        try {
            scheduleGenerator.applyPayment(
                    event.getLoanApplicationId(),
                    event.getEmiNumber(),
                    event.getAmountPaid(),
                    event.getPaymentId());
        } catch (Exception ex) {
            log.error("[{}] Failed to apply payment for loan {} EMI #{}: {}",
                    event.getCorrelationId(),
                    event.getLoanApplicationId(),
                    event.getEmiNumber(), ex.getMessage(), ex);
        }
    }
}