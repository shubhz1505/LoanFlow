package com.loanflow.document.kafka;

import com.loanflow.commons.events.LoanApplicationSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEventConsumer {

    @KafkaListener(
            topics = "loan.application.submitted",
            groupId = "document-service-group")
    public void onLoanSubmitted(@Payload LoanApplicationSubmittedEvent event) {
        log.info("[{}] Loan {} submitted — ready to receive documents",
                event.getCorrelationId(), event.getLoanApplicationId());
    }
}