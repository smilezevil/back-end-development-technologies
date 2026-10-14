package com.smilezevil.hotelbooking.config;

import com.smilezevil.hotelbooking.service.ExchangeRateService;
import com.smilezevil.hotelbooking.service.InvoiceService;
import com.smilezevil.hotelbooking.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.Async;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
class AsyncExecutionTest {

    @Configuration
    @Import({AsyncConfig.class, InvoiceService.class, ExchangeRateService.class, NotificationService.class})
    static class Config {

        @Bean
        ThreadNameProbe threadNameProbe() {
            return new ThreadNameProbe();
        }
    }

    static class ThreadNameProbe {

        @Async(AsyncConfig.BOOKING_EXECUTOR)
        public CompletableFuture<String> onBookingExecutor() {
            return CompletableFuture.completedFuture(Thread.currentThread().getName());
        }

        @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
        public CompletableFuture<String> onNotificationExecutor() {
            return CompletableFuture.completedFuture(Thread.currentThread().getName());
        }

        @Async
        public CompletableFuture<String> onDefaultExecutor() {
            return CompletableFuture.completedFuture(Thread.currentThread().getName());
        }
    }

    @Autowired
    private ThreadNameProbe threadNameProbe;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private NotificationService notificationService;


    @Test
    void asyncMethod_bookingExecutorName_runsOnBookingThread() {
        String threadName = threadNameProbe.onBookingExecutor().join();

        assertThat(threadName)
                .startsWith("booking-async-")
                .isNotEqualTo(Thread.currentThread().getName());
    }

    @Test
    void asyncMethod_notificationExecutorName_runsOnNotifyThread() {
        String threadName = threadNameProbe.onNotificationExecutor().join();

        assertThat(threadName).startsWith("notify-");
    }

    @Test
    void asyncMethod_noExecutorName_usesDefaultFromAsyncConfigurer() {
        String threadName = threadNameProbe.onDefaultExecutor().join();

        assertThat(threadName).startsWith("booking-async-");
    }

    @Test
    void generateInvoicePdf_called_returnsImmediatelyAndCompletesLater() {
        long start = System.currentTimeMillis();
        CompletableFuture<String> future = invoiceService.generateInvoicePdf(7L);
        long callTime = System.currentTimeMillis() - start;

        assertThat(callTime).isLessThan(500);
        assertThat(future).isNotDone();
        assertThat(future.join()).isEqualTo("invoice-7.pdf");
    }

    @Test
    void sendBookingConfirmation_fireAndForget_doesNotBlockCaller() {
        long start = System.currentTimeMillis();
        notificationService.sendBookingConfirmation(7L, "guest@gmail.com");
        long callTime = System.currentTimeMillis() - start;

        assertThat(callTime).isLessThan(500);
    }

    @Test
    void invoiceAndExchangeRate_startedTogether_runInParallel() {
        long start = System.currentTimeMillis();
        CompletableFuture<String> invoice = invoiceService.generateInvoicePdf(7L);
        CompletableFuture<BigDecimal> eur = exchangeRateService.convertUahToEur(new BigDecimal("1200.00"));
        CompletableFuture.allOf(invoice, eur).join();
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isLessThan(1800);
        assertThat(invoice.join()).isEqualTo("invoice-7.pdf");
        assertThat(eur.join()).isEqualByComparingTo("26.67");
    }
}