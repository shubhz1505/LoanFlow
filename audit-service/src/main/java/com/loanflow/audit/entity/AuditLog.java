package com.loanflow.audit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs",
        indexes = {
                @Index(name = "idx_al_entity_id",   columnList = "entity_id"),
                @Index(name = "idx_al_actor_id",    columnList = "actor_id"),
                @Index(name = "idx_al_event_type",  columnList = "event_type"),
                @Index(name = "idx_al_created_at",  columnList = "created_at"),
                @Index(name = "idx_al_correlation", columnList = "correlation_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    private String id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "actor_role")
    private String actorRole;

    @Column(name = "actor_ip")
    private String actorIp;

    @Column(name = "previous_state", columnDefinition = "TEXT")
    private String previousState;

    @Column(name = "new_state", columnDefinition = "TEXT")
    private String newState;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "source_service")
    private String sourceService;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }
}