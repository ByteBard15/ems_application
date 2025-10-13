package com.bytebard.core.api.types;

import org.springframework.http.HttpStatus;

public class ApiResponse<T> {

    private T data;
    private final int status;
    private final boolean success;
    private String message;

    public ApiResponse(T data, HttpStatus status, boolean success) {
        this.data = data;
        this.status = status.value();
        this.success = success;
    }

    public ApiResponse(T data, HttpStatus status, boolean success, String message) {
        this.data = data;
        this.status = status.value();
        this.success = success;
        this.message = message;
    }

    public ApiResponse(HttpStatus status, boolean success, String message) {
        this.status = status.value();
        this.success = success;
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public int getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
