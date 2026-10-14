package com.smilezevil.hotelbooking.dto;

import java.math.BigDecimal;

public record BookingSummaryDTO(
        Long bookingId,
        String invoice,
        BigDecimal totalPriceUah,
        String totalPriceEur
) {
}