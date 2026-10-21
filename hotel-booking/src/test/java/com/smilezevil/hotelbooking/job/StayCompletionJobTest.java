package com.smilezevil.hotelbooking.job;

import com.smilezevil.hotelbooking.service.BookingMaintenanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionException;
import org.quartz.impl.JobExecutionContextImpl;

import java.util.Map;

import static com.smilezevil.hotelbooking.job.JobContexts.contextFor;
import static com.smilezevil.hotelbooking.job.StayCompletionJob.GRACE_DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StayCompletionJobTest {

    @Mock
    private BookingMaintenanceService bookingMaintenanceService;

    @InjectMocks
    private StayCompletionJob job;


    @Test
    void execute_defaultParameters_callsServiceOnce() throws Exception {
        JobExecutionContextImpl context = contextFor(StayCompletionJob.class, Map.of(GRACE_DAYS, 0), Map.of());

        job.execute(context);

        verify(bookingMaintenanceService).completeFinishedStays(0);
        verifyNoMoreInteractions(bookingMaintenanceService);
    }

    @Test
    void execute_serviceFails_recordsErrorAndWaitsForNextScheduledRun() {
        when(bookingMaintenanceService.completeFinishedStays(0))
                .thenThrow(new IllegalStateException("DB is down"));
        JobExecutionContextImpl context = contextFor(StayCompletionJob.class, Map.of(GRACE_DAYS, 0), Map.of());

        JobExecutionException exception = assertThrows(JobExecutionException.class, () -> job.execute(context));

        assertThat(exception.refireImmediately()).isFalse();
        assertThat(exception.unscheduleAllTriggers()).isFalse();
        assertThat(exception.getCause()).hasMessage("DB is down");
        verify(bookingMaintenanceService).completeFinishedStays(0);
    }

    @Test
    void execute_keyFromAnotherJob_throwsExplicitError() {
        JobExecutionContextImpl context = contextFor(StayCompletionJob.class,
                Map.of(GRACE_DAYS, 0, "maxRetries", 3), Map.of());

        assertThatThrownBy(() -> job.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("Unknown JobDataMap key(s) [maxRetries]");
        verifyNoInteractions(bookingMaintenanceService);
    }
}