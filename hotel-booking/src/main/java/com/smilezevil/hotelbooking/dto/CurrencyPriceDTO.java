package com.smilezevil.hotelbooking.dto;

import java.math.BigDecimal;

public record CurrencyPriceDTO(Long id, BigDecimal amountUah, String currency, BigDecimal amount, String via) {
}