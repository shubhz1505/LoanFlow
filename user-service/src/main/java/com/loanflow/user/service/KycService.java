package com.loanflow.user.service;

import com.loanflow.commons.enums.KycStatus;
import com.loanflow.commons.events.UserKycVerifiedEvent;
import com.loanflow.commons.exceptions.InvalidStateException;
import com.loanflow.commons.exceptions.ResourceNotFoundException;
import com.loanflow.commons.kafka.EventPublisher;
import com.loanflow.user.dto.request.KycReviewRequest;
import com.loanflow.user.dto.request.KycSubmitRequest;
import com.loanflow.user.entity.KycAuditLog;
import com.loanflow.user.entity.User;
import com.loanflow.user.repository.KycAuditLogRepository;
import com.loanflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycService {

    private final UserRepository        userRepository;
    private final KycAuditLogRepository kycAuditLogRepository;
    private final EventPublisher        eventPublisher;

    private static final String TOPIC_KYC_VERIFIED = "user.kyc.verified";
    private static final int    MAX_KYC_ATTEMPTS   = 3;

    @Transactional
    public void submitKyc(String userId, KycSubmitRequest request) {
        User user = getUser(userId);

        if (user.getKycStatus() == KycStatus.VERIFIED) {
            throw new InvalidStateException("KYC already verified");
        }
        if (user.getKycStatus() == KycStatus.UNDER_REVIEW) {
            throw new InvalidStateException("KYC already under review");
        }
        if (user.getKycAttemptCount() >= MAX_KYC_ATTEMPTS) {
            throw new InvalidStateException(
                    "Maximum KYC attempts reached. Contact support.");
        }

        KycStatus previous = user.getKycStatus();

        user.setPanReference(request.getPanNumber().toUpperCase());
        user.setAadhaarReference("XXXX-XXXX-"
                + request.getAadhaarNumber().substring(8));
        user.setKycStatus(KycStatus.PENDING);
        user.setKycSubmittedAt(LocalDateTime.now());
        user.setKycAttemptCount(user.getKycAttemptCount() + 1);
        user.setKycRejectionReason(null);
        userRepository.save(user);

        recordAudit(userId, previous, KycStatus.PENDING,
                userId, "APPLICANT", "KYC submitted by user", null);

        user.setKycStatus(KycStatus.UNDER_REVIEW);
        userRepository.save(user);

        recordAudit(userId, KycStatus.PENDING, KycStatus.UNDER_REVIEW,
                "SYSTEM", "SYSTEM", "Auto-advanced to under review", null);

        log.info("KYC submitted userId={} attempt={}",
                userId, user.getKycAttemptCount());
    }

    @Transactional
    public void reviewKyc(String officerId, String officerRole,
                          KycReviewRequest request, String ipAddress) {

        User user = getUser(request.getUserId());

        if (user.getKycStatus() != KycStatus.UNDER_REVIEW
                && user.getKycStatus() != KycStatus.PENDING) {
            throw new InvalidStateException(
                    "KYC not pending review. Current: " + user.getKycStatus());
        }

        if (request.getDecision() == KycStatus.VERIFIED) {
            approveKyc(user, officerId, officerRole, ipAddress);
        } else if (request.getDecision() == KycStatus.REJECTED) {
            rejectKyc(user, officerId, officerRole,
                    request.getRejectionReason(), ipAddress);
        } else {
            throw new InvalidStateException(
                    "Invalid decision: " + request.getDecision());
        }
    }

    public List<KycAuditLog> getKycHistory(String userId) {
        return kycAuditLogRepository.findByUserIdOrderByCreatedAtAsc(userId);
    }

    private void approveKyc(User user, String officerId,
                            String officerRole, String ipAddress) {
        KycStatus previous = user.getKycStatus();
        user.setKycStatus(KycStatus.VERIFIED);
        user.setKycVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        recordAudit(user.getId(), previous, KycStatus.VERIFIED,
                officerId, officerRole, "KYC approved by officer", ipAddress);

        UserKycVerifiedEvent event = UserKycVerifiedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId("KYC-" + user.getId()
                        .substring(0, 8).toUpperCase())
                .occurredAt(LocalDateTime.now())
                .sourceService("user-service")
                .eventVersion("1.0")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .kycStatus(KycStatus.VERIFIED)
                .verifiedAt(LocalDateTime.now())
                .build();

        eventPublisher.publish(TOPIC_KYC_VERIFIED, user.getId(),
                event, event.getCorrelationId());

        log.info("KYC VERIFIED userId={} officerId={}", user.getId(), officerId);
    }

    private void rejectKyc(User user, String officerId,
                           String officerRole, String reason,
                           String ipAddress) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        KycStatus previous = user.getKycStatus();
        user.setKycStatus(KycStatus.REJECTED);
        user.setKycRejectionReason(reason);
        userRepository.save(user);

        recordAudit(user.getId(), previous, KycStatus.REJECTED,
                officerId, officerRole, reason, ipAddress);

        log.info("KYC REJECTED userId={} officerId={} reason={}",
                user.getId(), officerId, reason);
    }

    private void recordAudit(String userId,
                             KycStatus from, KycStatus to,
                             String performedBy, String performedByRole,
                             String remarks, String ipAddress) {
        kycAuditLogRepository.save(KycAuditLog.builder()
                .userId(userId)
                .fromStatus(from)
                .toStatus(to)
                .performedBy(performedBy)
                .performedByRole(performedByRole)
                .remarks(remarks)
                .ipAddress(ipAddress)
                .build());
    }

    private User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", userId));
    }
}