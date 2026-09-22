package com.loanflow.audit.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanflow.commons.events.*;
import com.loanflow.audit.entity.AuditLog;
import com.loanflow.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper       objectMapper;

    @KafkaListener(topics = "loan.application.submitted",
            groupId = "audit-service-group")
    public void onLoanSubmitted(@Payload LoanApplicationSubmittedEvent e) {
        persist("LOAN_SUBMITTED", "LOAN_APPLICATION",
                e.getLoanApplicationId(), e.getUserId(),
                toJson(e), e.getCorrelationId(), "loan-service");
    }

    @KafkaListener(topics = "loan.approved",
            groupId = "audit-service-group")
    public void onLoanApproved(@Payload LoanApprovedEvent e) {
        persist("LOAN_APPROVED", "LOAN_APPLICATION",
                e.getLoanApplicationId(), e.getApprovedByOfficerId(),
                toJson(e), e.getCorrelationId(), "loan-service");
    }

    @KafkaListener(topics = "loan.rejected",
            groupId = "audit-service-group")
    public void onLoanRejected(@Payload LoanRejectedEvent e) {
        persist("LOAN_REJECTED", "LOAN_APPLICATION",
                e.getLoanApplicationId(), e.getRejectedByOfficerId(),
                toJson(e), e.getCorrelationId(), "loan-service");
    }

    @KafkaListener(topics = "loan.credit.scored",
            groupId = "audit-service-group")
    public void onCreditScored(@Payload LoanCreditScoredEvent e) {
        persist("CREDIT_SCORED", "LOAN_APPLICATION",
                e.getLoanApplicationId(), "SYSTEM",
                toJson(e), e.getCorrelationId(), "credit-service");
    }

    @KafkaListener(topics = "payment.received",
            groupId = "audit-service-group")
    public void onPaymentReceived(@Payload PaymentReceivedEvent e) {
        persist("PAYMENT_RECEIVED", "PAYMENT",
                e.getPaymentId(), e.getUserId(),
                toJson(e), e.getCorrelationId(), "payment-service");
    }

    @KafkaListener(topics = "payment.overdue",
            groupId = "audit-service-group")
    public void onPaymentOverdue(@Payload PaymentOverdueEvent e) {
        persist("PAYMENT_OVERDUE", "LOAN_APPLICATION",
                e.getLoanApplicationId(), "SYSTEM",
                toJson(e), e.getCorrelationId(), "emi-service");
    }

    @KafkaListener(topics = "user.kyc.verified",
            groupId = "audit-service-group")
    public void onKycVerified(@Payload UserKycVerifiedEvent e) {
        persist("KYC_VERIFIED", "USER",
                e.getUserId(), e.getUserId(),
                toJson(e), e.getCorrelationId(), "user-service");
    }

    private void persist(String eventType, String entityType,
                         String entityId, String actorId,
                         String newState, String correlationId,
                         String sourceService) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .eventType(eventType)
                    .entityType(entityType)
                    .entityId(entityId != null ? entityId : "unknown")
                    .actorId(actorId)
                    .newState(newState)
                    .correlationId(correlationId)
                    .sourceService(sourceService)
                    .build());
        } catch (Exception ex) {
            log.error("Failed to persist audit for {}: {}",
                    eventType, ex.getMessage());
        }
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }
}