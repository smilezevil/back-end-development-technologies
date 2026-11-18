package com.smilezevil.hotelbooking.job;

import com.smilezevil.hotelbooking.repository.BookingRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;

@Component
public class BookingStatsReportJob {

    private static final Logger log = LoggerFactory.getLogger(BookingStatsReportJob.class);

    private final BookingRepository bookingRepository;
    private final String instance;

    public BookingStatsReportJob(BookingRepository bookingRepository, @Value("${server.port}") String port) {
        this.bookingRepository = bookingRepository;
        this.instance = "port " + port + ", pid " + ManagementFactory.getRuntimeMXBean().getPid();
    }

    @Scheduled(fixedRateString = "${reports.booking-stats.interval}")
    @SchedulerLock(name = "bookingStatsReport",
            lockAtMostFor = "${reports.booking-stats.lock-at-most-for}",
            lockAtLeastFor = "${reports.booking-stats.lock-at-least-for}")
    public void report() {
        log.info("BOOKING REPORT by instance [{}]: {} bookings in total", instance, bookingRepository.count());
    }
}