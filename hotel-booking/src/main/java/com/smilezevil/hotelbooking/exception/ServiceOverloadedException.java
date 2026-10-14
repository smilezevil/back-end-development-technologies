package com.smilezevil.hotelbooking.exception;

public class ServiceOverloadedException extends RuntimeException {

    public ServiceOverloadedException(String message, Throwable cause) {
        super(message, cause);
    }
}