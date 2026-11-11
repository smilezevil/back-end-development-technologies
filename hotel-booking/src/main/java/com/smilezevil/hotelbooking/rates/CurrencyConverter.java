package com.smilezevil.hotelbooking.rates;

import java.math.BigDecimal;

public interface CurrencyConverter {

    ConvertedAmount convert(BigDecimal amountUah, String currency);
}