package com.loanflow.audit.controller;

import com.loanflow.commons.dto.ApiResponse;
import com.loanflow.audit.entity.AuditLog;
import com.loanflow.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/entity/{entityId}")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getByEntity(
            @PathVariable String entityId) {

        return ResponseEntity.ok(ApiResponse.success(
                auditLogRepository
                        .findByEntityIdOrderByCreatedAtAsc(entityId)));
    }

    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getByCorrelation(
            @PathVariable String correlationId) {

        return ResponseEntity.ok(ApiResponse.success(
                auditLogRepository.findByCorrelationId(correlationId)));
    }
}