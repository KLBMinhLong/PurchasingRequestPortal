package com.fis.purchasing.controller;

import com.fis.purchasing.dto.request.LoginRequest;
import com.fis.purchasing.dto.request.LogoutRequest;
import com.fis.purchasing.dto.response.ApiResponse;
import com.fis.purchasing.dto.response.LoginResponse;
import com.fis.purchasing.dto.response.LogoutResponse;
import com.fis.purchasing.service.LoginService;
import com.fis.purchasing.service.LogoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginService loginService;
    private final LogoutService logoutService;

    public AuthController(LoginService loginService, LogoutService logoutService) {
        this.loginService = loginService;
        this.logoutService = logoutService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = loginService.login(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("SUCCESS", "Login successful", response, "/api/auth/login"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(@Valid @RequestBody(required = false) LogoutRequest request) {
        LogoutRequest logoutRequest = request == null ? new LogoutRequest() : request;
        LogoutResponse response = logoutService.logout(logoutRequest);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("SUCCESS", "Logout completed successfully", response, "/api/auth/logout"));
    }
}
