package com.fis.purchasing.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UserUpdateRequest {

    @Size(max = 100)
    private String username;

    @Email
    @Size(max = 255)
    private String email;

    @Size(min = 8, max = 255)
    private String password;

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 30)
    private String phone;

    private Boolean active;

    private Boolean locked;

    private List<@Size(max = 50) String> roleCodes;

    private Map<String, Object> attributes;
}