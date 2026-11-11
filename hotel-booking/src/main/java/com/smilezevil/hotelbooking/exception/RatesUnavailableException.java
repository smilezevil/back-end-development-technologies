package com.smilezevil.hotelbooking.exception;

public class RatesUnavailableException extends RuntimeException {

    public RatesUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}