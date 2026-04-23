package com.fis.purchasing.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class LoginUserResponse {
    String id;
    String username;
    String email;
    List<String> roles;
}
