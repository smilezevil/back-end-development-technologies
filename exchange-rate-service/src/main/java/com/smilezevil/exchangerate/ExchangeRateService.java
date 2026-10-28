package com.smilezevil.exchangerate;

import com.smilezevil.common.dto.ConversionResponse;
import com.smilezevil.common.dto.ExchangeRateResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

@Service
public class ExchangeRateService {

    private static final Map<String, BigDecimal> UAH_PER_UNIT = Map.of(
            "EUR", new BigDecimal("45.00"),
            "USD", new BigDecimal("41.50"),
            "PLN", new BigDecimal("10.40"));

    public ExchangeRateResponse rateFor(String currency) {
        String code = normalize(currency);
        return new ExchangeRateResponse(code, uahPerUnit(code));
    }

    public ConversionResponse convertFromUah(BigDecimal amountUah, String currency) {
        if (amountUah == null || amountUah.signum() < 0) {
            throw new IllegalArgumentException("Amount in UAH must not be negative");
        }
        String code = normalize(currency);
        BigDecimal amount = amountUah.divide(uahPerUnit(code), 2, RoundingMode.HALF_UP);
        return new ConversionResponse(amountUah, code, amount);
    }

    private BigDecimal uahPerUnit(String code) {
        BigDecimal rate = UAH_PER_UNIT.get(code);
        if (rate == null) {
            throw new UnknownCurrencyException("Unknown currency '" + code + "', supported: "
                    + new TreeSet<>(UAH_PER_UNIT.keySet()));
        }
        return rate;
    }

    private String normalize(String currency) {
        return currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
    }
}