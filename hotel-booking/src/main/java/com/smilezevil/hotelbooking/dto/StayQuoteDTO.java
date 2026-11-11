package com.smilezevil.hotelbooking.dto;

import java.math.BigDecimal;

public record StayQuoteDTO(Long roomId, int nights, BigDecimal totalUah, String currency,
                           BigDecimal uahPerUnit, BigDecimal total, String via) {
}