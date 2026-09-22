package com.loanflow.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notif_user_id", columnList = "user_id"),
                @Index(name = "idx_notif_loan_id", columnList = "loan_application_id"),
                @Index(name = "idx_notif_type",    columnList = "notification_type"),
                @Index(name = "idx_notif_status",  columnList = "status")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "loan_application_id")
    private String loanApplicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationChannel channel = NotificationChannel.EMAIL;

    @Column(name = "recipient_address", nullable = false)
    private String recipientAddress;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "correlation_id")
    private String correlationId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.retryCount == null) this.retryCount = 0;
        if (this.status == null) this.status = NotificationStatus.PENDING;
        if (this.channel == null) this.channel = NotificationChannel.EMAIL;
    }

    public enum NotificationType {
        REGISTRATION_WELCOME,
        KYC_SUBMITTED, KYC_APPROVED, KYC_REJECTED,
        LOAN_SUBMITTED, LOAN_APPROVED, LOAN_REJECTED, LOAN_DISBURSED,
        EMI_REMINDER, PAYMENT_RECEIVED,
        PAYMENT_OVERDUE, NPA_ALERT
    }

    public enum NotificationChannel { EMAIL, SMS, PUSH }

    public enum NotificationStatus { PENDING, SENT, FAILED, SKIPPED }
}