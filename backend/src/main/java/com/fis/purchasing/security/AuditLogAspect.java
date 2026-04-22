package com.fis.purchasing.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fis.purchasing.service.AuditLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

@Aspect
@Component
public class AuditLogAspect {

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password",
            "passwordHash",
            "refreshToken",
            "idTokenHint",
            "clientSecret",
            "token",
            "challengeResponse"
    );

    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    public AuditLogAspect(ObjectMapper objectMapper, AuditLogService auditLogService) {
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
    }

    @Around("@annotation(auditChange)")
    public Object around(ProceedingJoinPoint joinPoint, AuditChange auditChange) throws Throwable {
        String beforeData = serialize(joinPoint.getArgs());
        Object result = joinPoint.proceed();
        String afterData = serialize(result);
        UUID entityId = resolveEntityId(result, joinPoint.getArgs());
        auditLogService.record(auditChange.action(), auditChange.entity(), entityId, beforeData, afterData);
        return result;
    }

    private String serialize(Object value) {
        try {
            JsonNode node = objectMapper.valueToTree(value);
            JsonNode sanitized = sanitize(node);
            return objectMapper.writeValueAsString(sanitized);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private JsonNode sanitize(JsonNode node) {
        if (node == null || node.isNull()) {
            return NullNode.instance;
        }
        if (node.isObject()) {
            ObjectNode objectNode = ((ObjectNode) node).deepCopy();
            objectNode.fieldNames().forEachRemaining(fieldName -> {
                if (SENSITIVE_FIELDS.contains(fieldName)) {
                    objectNode.put(fieldName, "[REDACTED]");
                } else {
                    objectNode.set(fieldName, sanitize(objectNode.get(fieldName)));
                }
            });
            return objectNode;
        }
        if (node.isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            node.forEach(item -> arrayNode.add(sanitize(item)));
            return arrayNode;
        }
        return node;
    }

    private UUID resolveEntityId(Object result, Object[] args) {
        UUID fromResult = tryResolveId(result);
        if (fromResult != null) {
            return fromResult;
        }
        for (Object arg : args) {
            if (arg instanceof UUID uuid) {
                return uuid;
            }
        }
        return null;
    }

    private UUID tryResolveId(Object value) {
        if (value == null) {
            return null;
        }
        try {
            Method method = value.getClass().getMethod("getId");
            Object id = method.invoke(value);
            if (id instanceof UUID uuid) {
                return uuid;
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }
}