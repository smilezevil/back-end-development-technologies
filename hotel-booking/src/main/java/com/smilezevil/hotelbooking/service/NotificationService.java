package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.config.AsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    private static final long SMTP_DELAY_MS = 1000;

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    public void sendBookingConfirmation(Long bookingId, String email) {
        log.info("Sending booking confirmation for booking {}...", bookingId);
        simulateSmtpCall();
        if (email == null || email.endsWith(".invalid")) {
            throw new IllegalStateException("SMTP server rejected recipient for booking " + bookingId);
        }
        log.info("Booking confirmation for booking {} sent", bookingId);
    }

    private void simulateSmtpCall() {
        try {
            Thread.sleep(SMTP_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}