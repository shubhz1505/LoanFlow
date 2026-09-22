package com.loanflow.notification.kafka;

import com.loanflow.commons.events.*;
import com.loanflow.notification.entity.Notification;
import com.loanflow.notification.repository.NotificationRepository;
import com.loanflow.notification.service.EmailService;
import com.loanflow.notification.service.EmailTemplateBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final EmailService           emailService;
    private final EmailTemplateBuilder   templateBuilder;
    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = "loan.approved",
            groupId = "notification-service-group")
    public void onLoanApproved(@Payload LoanApprovedEvent event) {
        log.info("[{}] Sending approval notification for loan {}",
                event.getCorrelationId(), event.getLoanApplicationId());

        String body = templateBuilder.loanApproved(
                "Valued Customer",
                event.getLoanApplicationId(),
                event.getApprovedAmount(),
                event.getEmiAmount(),
                event.getInterestRate(),
                event.getTenureMonths(),
                event.getDisbursementDate());

        send(event.getUserId(), event.getLoanApplicationId(),
                event.getUserEmail(),
                "🎉 Loan Approved — " + event.getLoanApplicationId(),
                body,
                Notification.NotificationType.LOAN_APPROVED,
                event.getCorrelationId());
    }

    @KafkaListener(topics = "loan.rejected",
            groupId = "notification-service-group")
    public void onLoanRejected(@Payload LoanRejectedEvent event) {
        log.info("[{}] Sending rejection notification for loan {}",
                event.getCorrelationId(), event.getLoanApplicationId());

        String body = templateBuilder.loanRejected(
                "Valued Customer",
                event.getLoanApplicationId(),
                event.getRejectionReasons() != null
                        ? event.getRejectionReasons()
                        : java.util.List.of());

        send(event.getUserId(), event.getLoanApplicationId(),
                event.getUserEmail(),
                "Loan Application Update — " + event.getLoanApplicationId(),
                body,
                Notification.NotificationType.LOAN_REJECTED,
                event.getCorrelationId());
    }

    @KafkaListener(topics = "emi.due.reminder",
            groupId = "notification-service-group")
    public void onEmiReminder(@Payload EmiDueReminderEvent event) {
        log.info("[{}] Sending EMI reminder for loan {}",
                event.getCorrelationId(), event.getLoanApplicationId());

        String body = templateBuilder.emiReminder(
                "Valued Customer",
                event.getLoanApplicationId(),
                event.getEmiAmount(),
                event.getDueDate(),
                event.getDaysUntilDue(),
                event.getOutstandingBalance() != null
                        ? event.getOutstandingBalance()
                        : BigDecimal.ZERO);

        send(event.getUserId(), event.getLoanApplicationId(),
                event.getUserEmail(),
                "EMI Due in " + event.getDaysUntilDue() + " days",
                body,
                Notification.NotificationType.EMI_REMINDER,
                event.getCorrelationId());
    }

    @KafkaListener(topics = "payment.received",
            groupId = "notification-service-group")
    public void onPaymentReceived(@Payload PaymentReceivedEvent event) {
        log.info("[{}] Sending payment confirmation for loan {}",
                event.getCorrelationId(), event.getLoanApplicationId());

        String body = templateBuilder.paymentReceived(
                "Valued Customer",
                event.getLoanApplicationId(),
                event.getAmountPaid(),
                event.getOutstandingBalance());

        send(event.getUserId(), event.getLoanApplicationId(),
                event.getUserEmail(),
                "✅ Payment of ₹" + event.getAmountPaid() + " Received",
                body,
                Notification.NotificationType.PAYMENT_RECEIVED,
                event.getCorrelationId());
    }

    @KafkaListener(topics = "payment.overdue",
            groupId = "notification-service-group")
    public void onPaymentOverdue(@Payload PaymentOverdueEvent event) {
        log.warn("[{}] Sending overdue alert for loan {}",
                event.getCorrelationId(), event.getLoanApplicationId());

        String body = templateBuilder.paymentOverdue(
                "Valued Customer",
                event.getLoanApplicationId(),
                event.getOverdueAmount(),
                event.getOverdueDays(),
                event.getPenaltyAmount() != null
                        ? event.getPenaltyAmount()
                        : BigDecimal.ZERO);

        send(event.getUserId(), event.getLoanApplicationId(),
                event.getUserEmail(),
                "⚠️ URGENT: Overdue Payment — "
                        + event.getLoanApplicationId(),
                body,
                Notification.NotificationType.PAYMENT_OVERDUE,
                event.getCorrelationId());
    }

    @KafkaListener(topics = "user.kyc.verified",
            groupId = "notification-service-group")
    public void onKycResult(@Payload UserKycVerifiedEvent event) {
        log.info("[{}] Sending KYC notification to {}",
                event.getCorrelationId(), event.getEmail());

        boolean approved = "VERIFIED".equals(event.getKycStatus().name());
        String body = templateBuilder.kycResult(
                event.getFullName(), approved, null);

        send(event.getUserId(), null,
                event.getEmail(),
                approved ? "✅ KYC Verified" : "KYC Status Update",
                body,
                approved ? Notification.NotificationType.KYC_APPROVED
                        : Notification.NotificationType.KYC_REJECTED,
                event.getCorrelationId());
    }

    private void send(String userId, String loanId,
                      String email, String subject, String body,
                      Notification.NotificationType type,
                      String correlationId) {

        if (email == null || email.isBlank()) {
            log.warn("No email address for userId={} — skipping notification",
                    userId);
            return;
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .loanApplicationId(loanId)
                .notificationType(type)
                .channel(Notification.NotificationChannel.EMAIL)
                .recipientAddress(email)
                .subject(subject)
                .body(body)
                .correlationId(correlationId)
                .build();

        notificationRepository.save(notification);
        emailService.sendEmail(notification);
    }
}