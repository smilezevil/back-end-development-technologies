package com.smilezevil.hotelbooking.job;

import com.smilezevil.hotelbooking.repository.BookingRepository;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
@TestPropertySource(properties = {
        "server.port=8083",
        "reports.booking-stats.lock-at-most-for=PT30S",
        "reports.booking-stats.lock-at-least-for=PT9S"
})
@ExtendWith(OutputCaptureExtension.class)
class BookingStatsReportJobTest {

    @Configuration
    @EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
    @Import(BookingStatsReportJob.class)
    static class Config {

        @Bean
        BookingRepository bookingRepository() {
            return mock(BookingRepository.class);
        }

        @Bean
        LockProvider lockProvider() {
            return mock(LockProvider.class);
        }
    }

    @Autowired
    private BookingStatsReportJob job;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private LockProvider lockProvider;

    @BeforeEach
    void resetMocks() {
        reset(bookingRepository, lockProvider);
    }

    @Test
    void lockIsFree_reportRuns_withLockSettingsFromConfiguration(CapturedOutput output) {
        SimpleLock lock = mock(SimpleLock.class);
        when(lockProvider.lock(any())).thenReturn(Optional.of(lock));
        when(bookingRepository.count()).thenReturn(12L);

        job.report();

        verify(bookingRepository).count();
        verify(lock).unlock();
        ArgumentCaptor<LockConfiguration> captor = ArgumentCaptor.forClass(LockConfiguration.class);
        verify(lockProvider).lock(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("bookingStatsReport");
        assertThat(captor.getValue().getLockAtMostFor()).isEqualTo(Duration.ofSeconds(30));
        assertThat(captor.getValue().getLockAtLeastFor()).isEqualTo(Duration.ofSeconds(9));
        assertThat(output)
                .contains("BOOKING REPORT by instance [port 8083")
                .contains("12 bookings in total");
    }

    @Test
    void lockHeldByAnotherInstance_reportIsSkipped() {
        when(lockProvider.lock(any())).thenReturn(Optional.empty());

        job.report();

        verify(lockProvider).lock(any());
        verify(bookingRepository, never()).count();
    }
}