package com.loanflow.audit.repository;

import com.loanflow.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByEntityIdOrderByCreatedAtAsc(String entityId);

    Page<AuditLog> findByActorId(String actorId, Pageable pageable);

    Page<AuditLog> findByEventType(String eventType, Pageable pageable);

    List<AuditLog> findByCorrelationId(String correlationId);
}