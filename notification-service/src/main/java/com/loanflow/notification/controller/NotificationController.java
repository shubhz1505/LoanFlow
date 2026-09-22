package com.loanflow.notification.controller;

import com.loanflow.commons.dto.ApiResponse;
import com.loanflow.notification.entity.Notification;
import com.loanflow.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Notification>>> getUserNotifications(
            @PathVariable String userId) {

        return ResponseEntity.ok(ApiResponse.success(
                notificationRepository
                        .findByUserIdOrderByCreatedAtDesc(userId)));
    }
}