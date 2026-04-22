package com.fis.purchasing.service;

import com.fis.purchasing.entity.AuditLogEntity;
import com.fis.purchasing.repository.jpa.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String entityName, UUID entityId, String beforeData, String afterData) {
        AuditLogEntity entity = AuditLogEntity.builder()
                .actorUserId(resolveActorUserId())
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .requestId(resolveRequestId())
                .traceId(resolveHeader("X-Trace-Id"))
                .ipAddress(resolveClientIp())
                .userAgent(resolveHeader(HttpHeaders.USER_AGENT))
                .beforeData(beforeData)
                .afterData(afterData)
                .build();
        auditLogRepository.save(entity);
    }

    private UUID resolveActorUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            try {
                return UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private UUID resolveRequestId() {
        String requestId = resolveHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(requestId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String resolveClientIp() {
        String forwardedFor = resolveHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            return request.getRemoteAddr();
        }
        return null;
    }

    private String resolveHeader(String headerName) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest().getHeader(headerName);
        }
        return null;
    }
}