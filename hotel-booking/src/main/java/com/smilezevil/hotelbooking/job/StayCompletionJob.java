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
public class StayCompletionJob implements Job {

    public static final String GRACE_DAYS = "graceDays";
    static final Set<String> KNOWN_KEYS = Set.of(GRACE_DAYS);

    private final BookingMaintenanceService bookingMaintenanceService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();
        JobParameters.requireOnlyKnownKeys(data, KNOWN_KEYS);
        int graceDays = JobParameters.requiredNonNegativeInt(data, GRACE_DAYS);

        try {
            bookingMaintenanceService.completeFinishedStays(graceDays);
        } catch (RuntimeException e) {
            log.error("StayCompletionJob failed, waiting for the next scheduled run: {}", e.getMessage());
            throw new JobExecutionException(e, false);
        }
    }
}