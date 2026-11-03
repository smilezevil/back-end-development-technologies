package com.smilezevil.notification;

public class RecipientRejectedException extends RuntimeException {

    public RecipientRejectedException(String message) {
        super(message);
    }
}