package com.smilezevil.hotelbooking.rates;

import java.math.BigDecimal;

public record ConvertedAmount(BigDecimal amountUah, String currency, BigDecimal amount, String via) {
}