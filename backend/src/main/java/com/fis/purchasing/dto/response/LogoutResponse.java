package com.fis.purchasing.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LogoutResponse {
    boolean refreshTokenRevoked;
    boolean keycloakSessionLoggedOut;
    String message;
}