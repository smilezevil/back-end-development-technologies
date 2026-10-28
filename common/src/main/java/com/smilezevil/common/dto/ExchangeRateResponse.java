package com.smilezevil.common.dto;

import java.math.BigDecimal;

public record ExchangeRateResponse(String currency, BigDecimal uahPerUnit) {
}