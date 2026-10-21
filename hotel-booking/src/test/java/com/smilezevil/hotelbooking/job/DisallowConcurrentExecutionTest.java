package com.smilezevil.hotelbooking.job;

import com.smilezevil.hotelbooking.service.BookingMaintenanceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DisallowConcurrentExecutionTest {

    private static final long JOB_DURATION_MS = 300;
    private static final int RUNS = 3;

    private final AtomicInteger running = new AtomicInteger();
    private final AtomicInteger maxRunning = new AtomicInteger();
    private final CountDownLatch finished = new CountDownLatch(RUNS);

    private Scheduler scheduler;

    static class SameWorkWithoutAnnotationJob implements Job {

        private final BookingMaintenanceService service;

        SameWorkWithoutAnnotationJob(BookingMaintenanceService service) {
            this.service = service;
        }

        @Override
        public void execute(JobExecutionContext context) {
            service.cancelStalePendingBookings(0);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        BookingMaintenanceService slowService = mock(BookingMaintenanceService.class);
        when(slowService.cancelStalePendingBookings(anyInt())).thenAnswer(invocation -> {
            maxRunning.accumulateAndGet(running.incrementAndGet(), Math::max);
            Thread.sleep(JOB_DURATION_MS);
            running.decrementAndGet();
            finished.countDown();
            return 0;
        });

        Properties properties = new Properties();
        properties.setProperty("org.quartz.scheduler.instanceName", "concurrency-test-scheduler");
        properties.setProperty("org.quartz.threadPool.threadCount", "3");
        properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");

        scheduler = new StdSchedulerFactory(properties).getScheduler();
        scheduler.setJobFactory((bundle, quartzScheduler) ->
                bundle.getJobDetail().getJobClass() == BookingCleanupJob.class
                        ? new BookingCleanupJob(slowService)
                        : new SameWorkWithoutAnnotationJob(slowService));
    }

    @AfterEach
    void tearDown() throws Exception {
        scheduler.shutdown(true);
    }


    @Test
    void bookingCleanupJob_firedThreeTimesAtOnce_runsOneAfterAnother() throws Exception {
        long elapsed = fireThreeTimesAtOnce(BookingCleanupJob.class);

        assertThat(maxRunning.get()).isEqualTo(1);
        assertThat(elapsed).isGreaterThanOrEqualTo(RUNS * JOB_DURATION_MS);
    }

    @Test
    void jobWithoutAnnotation_firedThreeTimesAtOnce_overlaps() throws Exception {
        fireThreeTimesAtOnce(SameWorkWithoutAnnotationJob.class);

        assertThat(maxRunning.get()).isGreaterThan(1);
    }


    private long fireThreeTimesAtOnce(Class<? extends Job> jobClass) throws Exception {
        JobDetail job = JobBuilder.newJob(jobClass)
                .withIdentity(jobClass.getSimpleName(), "concurrency-test")
                .usingJobData(BookingCleanupJob.GRACE_DAYS, 0)
                .usingJobData(BookingCleanupJob.MAX_RETRIES, 0)
                .storeDurably()
                .build();
        scheduler.addJob(job, false);

        long start = System.currentTimeMillis();
        scheduler.start();
        for (int i = 0; i < RUNS; i++) {
            scheduler.triggerJob(job.getKey());
        }
        assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
        return System.currentTimeMillis() - start;
    }
}