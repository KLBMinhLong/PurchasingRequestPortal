package com.fis.purchasing.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fis.purchasing.dto.response.UserResponse;
import com.fis.purchasing.entity.RoleEntity;
import com.fis.purchasing.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component
public class UserMapper {

    private final ObjectMapper objectMapper;

    public UserMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public UserResponse toResponse(UserEntity entity) {
        return UserResponse.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .phone(entity.getPhone())
                .active(entity.isActive())
                .locked(entity.isLocked())
                .failedLoginAttempts(entity.getFailedLoginAttempts())
                .lastLoginAt(entity.getLastLoginAt())
                .attributes(toAttributes(entity.getAttributes()))
                .roleCodes(toRoleCodes(entity.getRoles()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public String toAttributeJson(Map<String, Object> attributes) {
        try {
            if (attributes == null || attributes.isEmpty()) {
                return "{}";
            }
            return objectMapper.writeValueAsString(attributes);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize user attributes", exception);
        }
    }

    public Map<String, Object> toAttributes(String attributesJson) {
        try {
            if (attributesJson == null || attributesJson.isBlank()) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(attributesJson, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to deserialize user attributes", exception);
        }
    }

    private Set<String> toRoleCodes(Set<RoleEntity> roles) {
        Set<String> roleCodes = new LinkedHashSet<>();
        if (roles == null) {
            return roleCodes;
        }
        for (RoleEntity role : roles) {
            roleCodes.add(role.getCode());
        }
        return roleCodes;
    }
}