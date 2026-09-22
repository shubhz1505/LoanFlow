package com.loanflow.notification.repository;

import com.loanflow.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, String> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Notification> findByStatusAndRetryCountLessThan(
            Notification.NotificationStatus status, int maxRetries);
}