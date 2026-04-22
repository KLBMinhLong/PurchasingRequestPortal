package com.fis.purchasing.service;

import com.fis.purchasing.config.KeycloakProperties;
import com.fis.purchasing.dto.request.LogoutRequest;
import com.fis.purchasing.dto.response.LogoutResponse;
import com.fis.purchasing.exception.BusinessRuleException;
import com.fis.purchasing.exception.ExternalServiceException;
import com.fis.purchasing.exception.ResourceNotFoundException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class LogoutService {

    private final RestClient restClient;
    private final KeycloakProperties keycloakProperties;

    public LogoutService(RestClient.Builder restClientBuilder, KeycloakProperties keycloakProperties) {
        this.restClient = restClientBuilder.build();
        this.keycloakProperties = keycloakProperties;
    }

    public LogoutResponse logout(LogoutRequest request) {
        JwtAuthenticationToken authentication = currentAuthentication();
        boolean refreshTokenRevoked = false;
        if (StringUtils.hasText(request.getRefreshToken())) {
            revokeRefreshToken(request.getRefreshToken());
            refreshTokenRevoked = true;
        }
        logoutUserSessions(authentication.getToken().getSubject());
        return LogoutResponse.builder()
                .refreshTokenRevoked(refreshTokenRevoked)
                .keycloakSessionLoggedOut(true)
                .message("Logout completed")
                .build();
    }

    private void revokeRefreshToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakProperties.clientId());
        form.add("client_secret", keycloakProperties.clientSecret());
        form.add("token", refreshToken);
        form.add("token_type_hint", "refresh_token");
        try {
            restClient.post()
                    .uri(keycloakBaseUrl() + "/realms/" + keycloakProperties.realm() + "/protocol/openid-connect/revoke")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            throw new ExternalServiceException("Failed to revoke refresh token in Keycloak");
        }
    }

    private void logoutUserSessions(String userId) {
        try {
            String adminAccessToken = requestAdminAccessToken();
            restClient.post()
                    .uri(keycloakBaseUrl() + "/admin/realms/" + keycloakProperties.realm() + "/users/" + userId + "/logout")
                    .headers(headers -> headers.setBearerAuth(adminAccessToken))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            throw new ExternalServiceException("Failed to revoke Keycloak session");
        }
    }

    private String requestAdminAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", keycloakProperties.clientId());
        form.add("client_secret", keycloakProperties.clientSecret());
        try {
            Map response = restClient.post()
                    .uri(keycloakBaseUrl() + "/realms/" + keycloakProperties.realm() + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            if (response == null || !response.containsKey("access_token")) {
                throw new ExternalServiceException("Keycloak did not return an admin token");
            }
            return String.valueOf(response.get("access_token"));
        } catch (Exception exception) {
            throw new ExternalServiceException("Failed to obtain Keycloak admin token");
        }
    }

    private JwtAuthenticationToken currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken;
        }
        throw new ResourceNotFoundException("Authenticated user is required for logout");
    }

    private String keycloakBaseUrl() {
        return keycloakProperties.baseUrl().replaceAll("/$", "");
    }
}