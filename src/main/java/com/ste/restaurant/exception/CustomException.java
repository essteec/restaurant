package com.ste.restaurant.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class CustomException extends RuntimeException {
    private final String error;
    private final int status;
    private final String detail;

    public CustomException(String message, String error, int status, String detail) {
        super(message);
        this.error = error;
        this.status = status;
        this.detail = detail;
    }
}
