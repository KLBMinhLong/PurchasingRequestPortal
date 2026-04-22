package com.fis.purchasing.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LogoutRequest {

    @Size(max = 4096)
    private String refreshToken;

    @Size(max = 4096)
    private String idTokenHint;

    @Size(max = 4096)
    private String postLogoutRedirectUri;
}