package com.loanflow.audit.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanflow.audit.annotation.Auditable;
import com.loanflow.audit.entity.AuditLog;
import com.loanflow.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Parameter;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper       objectMapper;

    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint,
                              Auditable auditable) throws Throwable {
        Object result    = null;
        Exception thrown = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception ex) {
            thrown = ex;
            throw ex;
        } finally {
            if (thrown == null) {
                try {
                    persistAuditLog(joinPoint, auditable, result);
                } catch (Exception auditEx) {
                    log.error("Audit persistence failed: {}",
                            auditEx.getMessage());
                }
            }
        }
    }

    private void persistAuditLog(ProceedingJoinPoint joinPoint,
                                 Auditable auditable,
                                 Object result) throws Exception {

        MethodSignature sig    = (MethodSignature) joinPoint.getSignature();
        Parameter[]     params = sig.getMethod().getParameters();
        Object[]        args   = joinPoint.getArgs();

        String entityId = extractEntityId(
                auditable.entityIdParam(), params, args);

        String actorId   = null;
        String actorRole = null;
        String actorIp   = null;

        try {
            var attrs = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                var req  = attrs.getRequest();
                actorId   = req.getHeader("X-User-Id");
                actorRole = req.getHeader("X-User-Role");
                actorIp   = req.getRemoteAddr();
            }
        } catch (Exception ignored) {}

        String newState = null;
        try {
            newState = objectMapper.writeValueAsString(result);
            if (newState != null && newState.length() > 10_000) {
                newState = newState.substring(0, 10_000) + "...[truncated]";
            }
        } catch (Exception ignored) {}

        auditLogRepository.save(AuditLog.builder()
                .eventType(auditable.eventType())
                .entityType(auditable.entityType())
                .entityId(entityId != null ? entityId : "unknown")
                .actorId(actorId)
                .actorRole(actorRole)
                .actorIp(actorIp)
                .newState(newState)
                .sourceService(sig.getDeclaringTypeName())
                .build());
    }

    private String extractEntityId(String paramName,
                                   Parameter[] params,
                                   Object[] args) {
        if (paramName == null || paramName.isBlank()) return null;
        for (int i = 0; i < params.length; i++) {
            if (params[i].getName().equals(paramName) && args[i] != null) {
                return args[i].toString();
            }
        }
        return null;
    }
}