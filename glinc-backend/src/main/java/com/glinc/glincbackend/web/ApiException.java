package com.glinc.glincbackend.web;

import org.springframework.http.HttpStatus;

// La traduce GlobalExceptionHandler a un Problem Details con este status y code.
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
