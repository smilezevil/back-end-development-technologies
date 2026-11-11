package com.smilezevil.hotelbooking.exception;

public class RateNotFoundException extends RuntimeException {

    public RateNotFoundException(String currency) {
        super("Курс для валюти " + currency + " не знайдено");
    }
}