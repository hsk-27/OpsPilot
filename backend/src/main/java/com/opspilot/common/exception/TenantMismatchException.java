package com.opspilot.common.exception;

import org.springframework.http.HttpStatus;

public class TenantMismatchException extends ApiException {
    public TenantMismatchException(String message) {
        super(message, HttpStatus.FORBIDDEN, "CROSS_TENANT_ACCESS_DENIED");
    }
}
