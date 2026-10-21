package com.smilezevil.hotelbooking.config;

import com.smilezevil.hotelbooking.job.BookingCleanupJob;
import com.smilezevil.hotelbooking.job.StayCompletionJob;
import com.smilezevil.hotelbooking.service.BookingMaintenanceService;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class QuartzConfig {

    public static final String MAINTENANCE_GROUP = "maintenance";
    public static final String LIFECYCLE_GROUP = "lifecycle";
    public static final TimeZone HOTEL_TIME_ZONE = TimeZone.getTimeZone(BookingMaintenanceService.HOTEL_ZONE);

    public static final JobKey CLEANUP_JOB_KEY =
            JobKey.jobKey("bookingCleanupJob", MAINTENANCE_GROUP);
    public static final TriggerKey CLEANUP_NIGHTLY_TRIGGER_KEY =
            TriggerKey.triggerKey("bookingCleanupNightlyTrigger", MAINTENANCE_GROUP);
    public static final TriggerKey CLEANUP_EVERY_MINUTE_TRIGGER_KEY =
            TriggerKey.triggerKey("bookingCleanupEveryMinuteTrigger", MAINTENANCE_GROUP);

    public static final JobKey STAY_COMPLETION_JOB_KEY =
            JobKey.jobKey("stayCompletionJob", LIFECYCLE_GROUP);
    public static final TriggerKey STAY_COMPLETION_TRIGGER_KEY =
            TriggerKey.triggerKey("stayCompletionTrigger", LIFECYCLE_GROUP);

    public static final String NIGHTLY_CLEANUP_CRON = "0 0 3 * * ?";
    public static final String STAY_COMPLETION_CRON = "0 */2 * * * ?";

    @Bean
    public JobDetail bookingCleanupJobDetail() {
        return JobBuilder.newJob(BookingCleanupJob.class)
                .withIdentity(CLEANUP_JOB_KEY)
                .withDescription("Cancels PENDING bookings whose check-in date has passed")
                .usingJobData(BookingCleanupJob.GRACE_DAYS, 1)
                .usingJobData(BookingCleanupJob.MAX_RETRIES, 2)
                .storeDurably()
                .build();
    }

    @Bean
    public CronTrigger bookingCleanupNightlyTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(CLEANUP_JOB_KEY)
                .withIdentity(CLEANUP_NIGHTLY_TRIGGER_KEY)
                .withDescription("Every night at 03:00 Kyiv time with more retries")
                .usingJobData(BookingCleanupJob.MAX_RETRIES, 5)
                .withSchedule(CronScheduleBuilder.cronSchedule(NIGHTLY_CLEANUP_CRON)
                        .inTimeZone(HOTEL_TIME_ZONE)
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    @Bean
    public SimpleTrigger bookingCleanupEveryMinuteTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(CLEANUP_JOB_KEY)
                .withIdentity(CLEANUP_EVERY_MINUTE_TRIGGER_KEY)
                .withDescription("Every minute, strict: no grace days")
                .usingJobData(BookingCleanupJob.GRACE_DAYS, 0)
                .withSchedule(SimpleScheduleBuilder.repeatMinutelyForever(1)
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    @Bean
    public JobDetail stayCompletionJobDetail() {
        return JobBuilder.newJob(StayCompletionJob.class)
                .withIdentity(STAY_COMPLETION_JOB_KEY)
                .withDescription("Marks CONFIRMED bookings with past check-out date as COMPLETED")
                .usingJobData(StayCompletionJob.GRACE_DAYS, 0)
                .storeDurably()
                .build();
    }

    @Bean
    public CronTrigger stayCompletionTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(STAY_COMPLETION_JOB_KEY)
                .withIdentity(STAY_COMPLETION_TRIGGER_KEY)
                .withDescription("Every 2 minutes, Kyiv time")
                .withSchedule(CronScheduleBuilder.cronSchedule(STAY_COMPLETION_CRON)
                        .inTimeZone(HOTEL_TIME_ZONE)
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }
}