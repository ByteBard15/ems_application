package com.bytebard.core.api.types;

import org.springframework.http.HttpStatus;

public class MvcApiReponse<T> {

    private T data;
    private final int status;
    private final boolean success;
    private String message;

    public MvcApiReponse(T data, HttpStatus status, boolean success) {
        this.data = data;
        this.status = status.value();
        this.success = success;
    }

    public MvcApiReponse(T data, HttpStatus status, boolean success, String message) {
        this.data = data;
        this.status = status.value();
        this.success = success;
        this.message = message;
    }

    public MvcApiReponse(HttpStatus status, boolean success, String message) {
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
