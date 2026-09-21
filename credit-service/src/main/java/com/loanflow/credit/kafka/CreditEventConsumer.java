package com.loanflow.credit.kafka;

import com.loanflow.commons.events.LoanApplicationSubmittedEvent;
import com.loanflow.credit.service.CreditScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreditEventConsumer {

    private final CreditScoringService creditScoringService;

    @KafkaListener(
            topics = "loan.application.submitted",
            groupId = "credit-service-group")
    public void onLoanSubmitted(
            @Payload LoanApplicationSubmittedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("[{}] Received loan.application.submitted for loan {} offset={}",
                event.getCorrelationId(), event.getLoanApplicationId(), offset);

        try {
            creditScoringService.scoreApplication(event);
        } catch (Exception ex) {
            log.error("[{}] Failed to score loan {}: {}",
                    event.getCorrelationId(),
                    event.getLoanApplicationId(), ex.getMessage(), ex);
        }
    }
}