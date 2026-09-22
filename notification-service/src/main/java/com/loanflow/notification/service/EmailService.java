package com.loanflow.notification.service;

import com.loanflow.notification.entity.Notification;
import com.loanflow.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender          mailSender;
    private final NotificationRepository  notificationRepository;

    @Async
    public void sendEmail(Notification notification) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(notification.getRecipientAddress());
            helper.setSubject(notification.getSubject());
            helper.setText(notification.getBody(), true);
            helper.setFrom("noreply@loanflow.com", "LoanFlow");

            mailSender.send(message);

            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            log.info("Email sent to {} type={}",
                    notification.getRecipientAddress(),
                    notification.getNotificationType());

        } catch (Exception e) {
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setFailureReason(e.getMessage());
            notification.setRetryCount(notification.getRetryCount() + 1);
            log.error("Failed to send email to {}: {}",
                    notification.getRecipientAddress(), e.getMessage());
        }
        notificationRepository.save(notification);
    }
}