package com.smilezevil.hotelbooking.rates;

import java.math.BigDecimal;

public record ExchangeRate(String currency, BigDecimal uahPerUnit, String via) {
}