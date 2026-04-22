package com.fis.purchasing.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends ApiException {

    public ExternalServiceException(String message) {
        super(HttpStatus.BAD_GATEWAY, "EXTERNAL_SERVICE_ERROR", message);
    }
}