package com.smilezevil.common.dto;

import java.math.BigDecimal;

public record ConversionResponse(BigDecimal amountUah, String currency, BigDecimal amount) {
}