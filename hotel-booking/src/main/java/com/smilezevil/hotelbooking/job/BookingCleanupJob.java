package com.smilezevil.hotelbooking.job;

import com.smilezevil.hotelbooking.service.BookingMaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Set;

@Slf4j
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class BookingCleanupJob implements Job {

    public static final String GRACE_DAYS = "graceDays";
    public static final String MAX_RETRIES = "maxRetries";
    static final Set<String> KNOWN_KEYS = Set.of(GRACE_DAYS, MAX_RETRIES);

    private final BookingMaintenanceService bookingMaintenanceService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();
        JobParameters.requireOnlyKnownKeys(data, KNOWN_KEYS);
        int graceDays = JobParameters.requiredNonNegativeInt(data, GRACE_DAYS);
        int maxRetries = JobParameters.requiredNonNegativeInt(data, MAX_RETRIES);

        try {
            bookingMaintenanceService.cancelStalePendingBookings(graceDays);
        } catch (RuntimeException e) {
            boolean retry = context.getRefireCount() < maxRetries;
            log.warn("BookingCleanupJob failed on attempt {} of {}, {}: {}",
                    context.getRefireCount() + 1, maxRetries + 1,
                    retry ? "retrying now" : "giving up until the next scheduled run", e.getMessage());
            throw new JobExecutionException(e, retry);
        }
    }
}