package com.loanflow.loan.kafka;

import com.loanflow.commons.events.LoanApplicationSubmittedEvent;
import com.loanflow.commons.events.LoanApprovedEvent;
import com.loanflow.commons.events.LoanRejectedEvent;
import com.loanflow.commons.kafka.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoanEventPublisher {

    private final EventPublisher eventPublisher;

    public static final String TOPIC_SUBMITTED = "loan.application.submitted";
    public static final String TOPIC_APPROVED  = "loan.approved";
    public static final String TOPIC_REJECTED  = "loan.rejected";

    public void publishSubmitted(LoanApplicationSubmittedEvent event) {
        eventPublisher.publish(TOPIC_SUBMITTED,
                event.getLoanApplicationId(), event, event.getCorrelationId());
    }

    public void publishApproved(LoanApprovedEvent event) {
        eventPublisher.publish(TOPIC_APPROVED,
                event.getLoanApplicationId(), event, event.getCorrelationId());
    }

    public void publishRejected(LoanRejectedEvent event) {
        eventPublisher.publish(TOPIC_REJECTED,
                event.getLoanApplicationId(), event, event.getCorrelationId());
    }
}