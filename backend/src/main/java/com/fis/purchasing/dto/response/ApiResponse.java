package com.fis.purchasing.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    String code;
    String message;
    T data;
    OffsetDateTime timestamp;
    String path;

    public static <T> ApiResponse<T> success(String code, String message, T data, String path) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .timestamp(OffsetDateTime.now())
                .path(path)
                .build();
    }

    public static <T> ApiResponse<T> failure(String code, String message, String path) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .timestamp(OffsetDateTime.now())
                .path(path)
                .build();
    }
}