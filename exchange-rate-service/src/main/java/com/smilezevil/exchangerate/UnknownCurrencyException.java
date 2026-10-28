package com.smilezevil.exchangerate;

public class UnknownCurrencyException extends RuntimeException {

    public UnknownCurrencyException(String message) {
        super(message);
    }
}