package com.fis.purchasing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fis.purchasing.config.KeycloakProperties;
import com.fis.purchasing.dto.request.LoginRequest;
import com.fis.purchasing.dto.response.LoginResponse;
import com.fis.purchasing.dto.response.LoginUserResponse;
import com.fis.purchasing.exception.ExternalServiceException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class LoginService {

    private final RestClient restClient;
    private final KeycloakProperties keycloakProperties;
    private final ObjectMapper objectMapper;

    public LoginService(RestClient.Builder restClientBuilder,
                        KeycloakProperties keycloakProperties,
                        ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.keycloakProperties = keycloakProperties;
        this.objectMapper = objectMapper;
    }

    public LoginResponse login(LoginRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloakProperties.clientId());
        form.add("client_secret", keycloakProperties.clientSecret());
        form.add("username", request.getUsername());
        form.add("password", request.getPassword());
        form.add("scope", "openid profile email");

        try {
            Map<?, ?> tokenResponse = restClient.post()
                    .uri(keycloakBaseUrl() + "/realms/" + keycloakProperties.realm() + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (tokenResponse == null || tokenResponse.get("access_token") == null) {
                throw new ExternalServiceException("Keycloak did not return access token");
            }

            String accessToken = String.valueOf(tokenResponse.get("access_token"));
            LoginUserResponse user = extractUserFromJwt(accessToken);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .tokenType(readString(tokenResponse, "token_type", "Bearer"))
                    .expiresIn(readInt(tokenResponse, "expires_in", 300))
                    .refreshToken(tokenResponse.get("refresh_token") == null ? null : String.valueOf(tokenResponse.get("refresh_token")))
                    .user(user)
                    .build();
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new ExternalServiceException("Invalid username or password");
        } catch (HttpClientErrorException exception) {
            String body = exception.getResponseBodyAsString();
            if (body != null && body.contains("invalid_client_credentials")) {
                throw new ExternalServiceException("Backend Keycloak client credentials are invalid");
            }
            if (body != null && body.contains("invalid_grant")) {
                throw new ExternalServiceException("Invalid username or password");
            }
            throw new ExternalServiceException("Keycloak authentication request failed");
        } catch (ExternalServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ExternalServiceException("Failed to authenticate with Keycloak");
        }
    }

    private LoginUserResponse extractUserFromJwt(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT payload");
            }
            byte[] decodedPayload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = objectMapper.readTree(new String(decodedPayload, StandardCharsets.UTF_8));

            List<String> roles = new ArrayList<>();
            JsonNode rolesNode = payload.get("roles");
            if (rolesNode != null && rolesNode.isArray()) {
                rolesNode.forEach(node -> roles.add(node.asText()));
            }

            return LoginUserResponse.builder()
                    .id(payload.path("sub").asText(""))
                    .username(payload.path("preferred_username").asText(""))
                    .email(payload.path("email").asText(""))
                    .roles(roles)
                    .build();
        } catch (Exception exception) {
            throw new ExternalServiceException("Failed to parse user claims from access token");
        }
    }

    private String keycloakBaseUrl() {
        return keycloakProperties.baseUrl().replaceAll("/$", "");
    }

    private String readString(Map<?, ?> payload, String key, String defaultValue) {
        Object value = payload.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private Integer readInt(Map<?, ?> payload, String key, int defaultValue) {
        Object value = payload.get(key);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
