package com.fis.purchasing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(String expectedAudience, List<String> corsAllowedOrigins) {
}