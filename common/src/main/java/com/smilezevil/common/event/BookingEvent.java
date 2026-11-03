package com.smilezevil.common.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingEvent(
        String eventId,
        BookingEventType type,
        Long bookingId,
        String guestEmail,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        BigDecimal totalPrice) {
}