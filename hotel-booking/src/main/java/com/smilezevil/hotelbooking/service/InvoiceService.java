package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.config.AsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class InvoiceService {

    private static final long PDF_RENDER_DELAY_MS = 1000;

    @Async(AsyncConfig.BOOKING_EXECUTOR)
    public CompletableFuture<String> generateInvoicePdf(Long bookingId) {
        log.info("Generating invoice PDF for booking {}...", bookingId);
        pause(PDF_RENDER_DELAY_MS);
        String fileName = "invoice-" + bookingId + ".pdf";
        log.info("Invoice {} generated", fileName);
        return CompletableFuture.completedFuture(fileName);
    }

    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}