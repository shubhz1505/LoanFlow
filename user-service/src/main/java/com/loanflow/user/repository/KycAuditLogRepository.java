package com.loanflow.user.repository;

import com.loanflow.user.entity.KycAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KycAuditLogRepository extends JpaRepository<KycAuditLog, String> {

    List<KycAuditLog> findByUserIdOrderByCreatedAtAsc(String userId);
}