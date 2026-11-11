package com.smilezevil.hotelbooking.rates;

public interface RateLookup {

    ExchangeRate rateFor(String currency);
}