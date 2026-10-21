package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingMaintenanceService {

    public static final ZoneId HOTEL_ZONE = ZoneId.of("Europe/Kyiv");
    public static final String PENDING = "PENDING";
    public static final String CANCELLED = "CANCELLED";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String COMPLETED = "COMPLETED";

    private final BookingRepository bookingRepository;

    @Transactional
    public int cancelStalePendingBookings(int graceDays) {
        LocalDate threshold = LocalDate.now(HOTEL_ZONE).minusDays(graceDays);
        int cancelled = bookingRepository.updateStatusWhereCheckInBefore(PENDING, CANCELLED, threshold);
        log.info("Cancelled {} stale PENDING booking(s) with check-in before {}", cancelled, threshold);
        return cancelled;
    }

    @Transactional
    public int completeFinishedStays(int graceDays) {
        LocalDate threshold = LocalDate.now(HOTEL_ZONE).minusDays(graceDays);
        int completed = bookingRepository.updateStatusWhereCheckOutBefore(CONFIRMED, COMPLETED, threshold);
        log.info("Completed {} finished stay(s) with check-out before {}", completed, threshold);
        return completed;
    }
}