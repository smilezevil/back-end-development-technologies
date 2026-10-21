package com.smilezevil.hotelbooking.config;

import com.smilezevil.hotelbooking.job.BookingCleanupJob;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class QuartzConfigTest {

    private final QuartzConfig config = new QuartzConfig();


    @Test
    void triggers_createdByConfig_pointToTheirJobByJobKey() {
        JobDetail cleanupJob = config.bookingCleanupJobDetail();
        JobDetail stayCompletionJob = config.stayCompletionJobDetail();

        assertThat(config.bookingCleanupNightlyTrigger().getJobKey()).isEqualTo(cleanupJob.getKey());
        assertThat(config.bookingCleanupEveryMinuteTrigger().getJobKey()).isEqualTo(cleanupJob.getKey());
        assertThat(config.stayCompletionTrigger().getJobKey()).isEqualTo(stayCompletionJob.getKey());
    }

    @Test
    void jobsAndTriggers_createdByConfig_haveExplicitNameAndGroup() {
        List<String> keys = List.of(
                config.bookingCleanupJobDetail().getKey().toString(),
                config.bookingCleanupNightlyTrigger().getKey().toString(),
                config.bookingCleanupEveryMinuteTrigger().getKey().toString(),
                config.stayCompletionJobDetail().getKey().toString(),
                config.stayCompletionTrigger().getKey().toString());

        assertThat(keys).containsExactly(
                "maintenance.bookingCleanupJob",
                "maintenance.bookingCleanupNightlyTrigger",
                "maintenance.bookingCleanupEveryMinuteTrigger",
                "lifecycle.stayCompletionJob",
                "lifecycle.stayCompletionTrigger");
    }

    @Test
    void mergedJobDataMap_triggerOverridesOneKey_triggerWinsAndOtherKeysKeepJobDefaults() {
        JobDetail cleanupJob = config.bookingCleanupJobDetail();

        JobDataMap everyMinute = mergeAsQuartzDoes(cleanupJob, config.bookingCleanupEveryMinuteTrigger());
        JobDataMap nightly = mergeAsQuartzDoes(cleanupJob, config.bookingCleanupNightlyTrigger());

        assertThat(everyMinute.getInt(BookingCleanupJob.GRACE_DAYS)).isEqualTo(0);
        assertThat(everyMinute.getInt(BookingCleanupJob.MAX_RETRIES)).isEqualTo(2);
        assertThat(nightly.getInt(BookingCleanupJob.GRACE_DAYS)).isEqualTo(1);
        assertThat(nightly.getInt(BookingCleanupJob.MAX_RETRIES)).isEqualTo(5);
        assertThat(cleanupJob.getJobDataMap().getInt(BookingCleanupJob.GRACE_DAYS)).isEqualTo(1);
    }

    @Test
    void triggers_createdByConfig_haveExplicitMisfireInstruction() {
        assertThat(config.bookingCleanupNightlyTrigger().getMisfireInstruction())
                .isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        assertThat(config.bookingCleanupEveryMinuteTrigger().getMisfireInstruction())
                .isEqualTo(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
        assertThat(config.stayCompletionTrigger().getMisfireInstruction())
                .isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
    }

    @Test
    void cronTriggers_createdByConfig_useKyivTimeZone() {
        assertThat(config.bookingCleanupNightlyTrigger().getTimeZone().getID()).isEqualTo("Europe/Kyiv");
        assertThat(config.stayCompletionTrigger().getTimeZone().getID()).isEqualTo("Europe/Kyiv");
    }

    @Test
    void nightlyTrigger_winterDate_firesAt3amKyivTimeRegardlessOfServerZone() {
        Date after = Date.from(Instant.parse("2030-01-15T12:00:00Z"));

        Date next = config.bookingCleanupNightlyTrigger().getFireTimeAfter(after);

        assertThat(next.toInstant()).isEqualTo(Instant.parse("2030-01-16T01:00:00Z"));
    }

    @Test
    void timeZone_unknownId_silentlyFallsBackToGmt() {
        TimeZone typo = TimeZone.getTimeZone("Europe/Kyyiv");

        assertThat(typo.getID()).isEqualTo("GMT");
    }


    private static JobDataMap mergeAsQuartzDoes(JobDetail job, Trigger trigger) {
        TriggerFiredBundle bundle = new TriggerFiredBundle(
                job, (OperableTrigger) trigger, null, false, new Date(), null, null, null);
        return new JobExecutionContextImpl(null, bundle, null).getMergedJobDataMap();
    }
}