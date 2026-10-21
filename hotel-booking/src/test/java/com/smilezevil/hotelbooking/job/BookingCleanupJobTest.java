package com.smilezevil.hotelbooking.job;

import com.smilezevil.hotelbooking.service.BookingMaintenanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.impl.JobExecutionContextImpl;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Map;

import static com.smilezevil.hotelbooking.job.BookingCleanupJob.GRACE_DAYS;
import static com.smilezevil.hotelbooking.job.BookingCleanupJob.MAX_RETRIES;
import static com.smilezevil.hotelbooking.job.JobContexts.contextFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCleanupJobTest {

    @Mock
    private BookingMaintenanceService bookingMaintenanceService;

    @InjectMocks
    private BookingCleanupJob job;


    @Test
    void execute_triggerOverridesGraceDays_callsServiceOnceWithTriggerValue() throws Exception {
        JobExecutionContextImpl context = contextFor(BookingCleanupJob.class,
                Map.of(GRACE_DAYS, 1, MAX_RETRIES, 2), Map.of(GRACE_DAYS, 0));

        job.execute(context);

        verify(bookingMaintenanceService).cancelStalePendingBookings(0);
        verifyNoMoreInteractions(bookingMaintenanceService);
    }

    @Test
    void execute_requiredKeyMissing_throwsExplicitErrorAndSkipsService() {
        JobExecutionContextImpl context = contextFor(BookingCleanupJob.class,
                Map.of(MAX_RETRIES, 2), Map.of());

        assertThatThrownBy(() -> job.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("Missing required JobDataMap key 'graceDays'");
        verifyNoInteractions(bookingMaintenanceService);
    }

    @Test
    void execute_keyMisspelledInTrigger_throwsExplicitErrorInsteadOfSilentDefault() {
        JobExecutionContextImpl context = contextFor(BookingCleanupJob.class,
                Map.of(GRACE_DAYS, 1, MAX_RETRIES, 2), Map.of("gracedays", 0));

        assertThatThrownBy(() -> job.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("Unknown JobDataMap key(s) [gracedays]");
        verifyNoInteractions(bookingMaintenanceService);
    }

    @Test
    void execute_negativeGraceDays_throwsExplicitError() {
        JobExecutionContextImpl context = contextFor(BookingCleanupJob.class,
                Map.of(GRACE_DAYS, -1, MAX_RETRIES, 2), Map.of());

        assertThatThrownBy(() -> job.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("must not be negative");
        verifyNoInteractions(bookingMaintenanceService);
    }

    @Test
    void execute_serviceFailsAndRetriesLeft_asksQuartzToRefireImmediately() {
        when(bookingMaintenanceService.cancelStalePendingBookings(1))
                .thenThrow(new IllegalStateException("DB is down"));
        JobExecutionContextImpl context = contextFor(BookingCleanupJob.class,
                Map.of(GRACE_DAYS, 1, MAX_RETRIES, 2), Map.of());

        JobExecutionException exception = assertThrows(JobExecutionException.class, () -> job.execute(context));

        assertThat(exception.refireImmediately()).isTrue();
        assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class).hasMessage("DB is down");
    }

    @Test
    void execute_serviceKeepsFailing_givesUpAfterMaxRetriesWithoutInfiniteLoop() {
        when(bookingMaintenanceService.cancelStalePendingBookings(1))
                .thenThrow(new IllegalStateException("DB is down"));
        JobExecutionContextImpl context = contextFor(BookingCleanupJob.class,
                Map.of(GRACE_DAYS, 1, MAX_RETRIES, 2), Map.of());

        int attempts = 0;
        boolean refire;
        do {
            attempts++;
            JobExecutionException exception = assertThrows(JobExecutionException.class, () -> job.execute(context));
            refire = exception.refireImmediately();
            if (refire) {
                context.incrementRefireCount();
            }
        } while (refire && attempts < 10);

        assertThat(attempts).isEqualTo(3);
        verify(bookingMaintenanceService, times(3)).cancelStalePendingBookings(1);
    }

    @Test
    void transactionBoundary_isOnServiceMethodNotOnJob() throws NoSuchMethodException {
        Method serviceMethod = BookingMaintenanceService.class.getMethod("cancelStalePendingBookings", int.class);
        Method jobMethod = BookingCleanupJob.class.getMethod("execute", JobExecutionContext.class);

        assertThat(serviceMethod.isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(jobMethod.isAnnotationPresent(Transactional.class)).isFalse();
        assertThat(BookingCleanupJob.class.isAnnotationPresent(Transactional.class)).isFalse();
    }
}