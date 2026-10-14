package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.config.AsyncConfig;
import com.smilezevil.hotelbooking.dto.BookingDTO;
import com.smilezevil.hotelbooking.exception.ServiceOverloadedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
class BookingSummaryOverloadTest {

    @Configuration
    @EnableAsync
    @Import({InvoiceService.class, ExchangeRateService.class, BookingSummaryService.class})
    static class Config {

        @Bean(name = AsyncConfig.BOOKING_EXECUTOR)
        ThreadPoolTaskExecutor tinyBookingExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setThreadNamePrefix("tiny-");
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setQueueCapacity(1);
            return executor;
        }
    }

    @MockBean
    private BookingService bookingService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private BookingSummaryService bookingSummaryService;


    @Test
    void getSummary_threadBusyAndQueueFull_throwsServiceOverloadedException() {
        BookingDTO booking = new BookingDTO();
        booking.setId(5L);
        booking.setTotalPrice(new BigDecimal("1200.00"));
        when(bookingService.findById(5L)).thenReturn(booking);

        CompletableFuture<String> running = invoiceService.generateInvoicePdf(1L);
        CompletableFuture<String> queued = invoiceService.generateInvoicePdf(2L);

        assertThatThrownBy(() -> bookingSummaryService.getSummary(5L))
                .isInstanceOf(ServiceOverloadedException.class)
                .hasMessage(BookingSummaryService.OVERLOADED)
                .hasCauseInstanceOf(TaskRejectedException.class);

        assertThat(running.join()).isEqualTo("invoice-1.pdf");
        assertThat(queued.join()).isEqualTo("invoice-2.pdf");
    }
}