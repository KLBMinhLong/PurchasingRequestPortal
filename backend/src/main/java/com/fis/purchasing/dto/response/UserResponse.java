package com.fis.purchasing.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Value
@Builder
public class UserResponse {
    UUID id;
    String username;
    String email;
    String firstName;
    String lastName;
    String phone;
    boolean active;
    boolean locked;
    short failedLoginAttempts;
    OffsetDateTime lastLoginAt;
    Map<String, Object> attributes;
    Set<String> roleCodes;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
    String createdBy;
    String updatedBy;
    OffsetDateTime deletedAt;
}