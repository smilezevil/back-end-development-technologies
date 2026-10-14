package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.config.AsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ExchangeRateService {

    private static final long API_DELAY_MS = 1000;
    private static final BigDecimal UAH_PER_EUR = new BigDecimal("45.00");

    @Value("${exchange-rate.api-available:true}")
    private boolean apiAvailable;

    @Async(AsyncConfig.BOOKING_EXECUTOR)
    public CompletableFuture<BigDecimal> convertUahToEur(BigDecimal amountUah) {
        log.info("Requesting EUR exchange rate from external API...");
        pause(API_DELAY_MS);
        if (!apiAvailable) {
            throw new IllegalStateException("Exchange rate API is unavailable");
        }
        BigDecimal amountEur = amountUah.divide(UAH_PER_EUR, 2, RoundingMode.HALF_UP);
        log.info("Converted {} UAH -> {} EUR", amountUah, amountEur);
        return CompletableFuture.completedFuture(amountEur);
    }

    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}