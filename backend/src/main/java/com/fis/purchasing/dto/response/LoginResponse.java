package com.fis.purchasing.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoginResponse {
    String accessToken;
    String tokenType;
    Integer expiresIn;
    String refreshToken;
    LoginUserResponse user;
}
