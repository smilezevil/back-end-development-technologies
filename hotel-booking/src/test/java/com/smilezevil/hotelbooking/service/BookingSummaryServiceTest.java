package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.dto.BookingDTO;
import com.smilezevil.hotelbooking.dto.BookingSummaryDTO;
import com.smilezevil.hotelbooking.exception.ServiceOverloadedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingSummaryServiceTest {

    private static final BigDecimal PRICE_UAH = new BigDecimal("1200.00");

    @Mock
    private BookingService bookingService;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private ExchangeRateService exchangeRateService;

    private BookingSummaryService bookingSummaryService;

    @BeforeEach
    void setUp() {
        bookingSummaryService = new BookingSummaryService(
                bookingService, invoiceService, exchangeRateService, 200);
        when(bookingService.findById(5L)).thenReturn(booking(5L));
    }


    @Test
    void getSummary_bothTasksSucceed_combinesInvoiceAndEurPrice() {
        when(invoiceService.generateInvoicePdf(5L))
                .thenReturn(CompletableFuture.completedFuture("invoice-5.pdf"));
        when(exchangeRateService.convertUahToEur(PRICE_UAH))
                .thenReturn(CompletableFuture.completedFuture(new BigDecimal("26.67")));

        BookingSummaryDTO result = bookingSummaryService.getSummary(5L);

        assertThat(result).isEqualTo(
                new BookingSummaryDTO(5L, "invoice-5.pdf", PRICE_UAH, "26.67 EUR"));
    }

    @Test
    void getSummary_invoiceGenerationFails_returnsFallbackInvoiceMessage() {
        when(invoiceService.generateInvoicePdf(5L))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("PDF engine crashed")));
        when(exchangeRateService.convertUahToEur(PRICE_UAH))
                .thenReturn(CompletableFuture.completedFuture(new BigDecimal("26.67")));

        BookingSummaryDTO result = bookingSummaryService.getSummary(5L);

        assertThat(result.invoice()).isEqualTo(BookingSummaryService.INVOICE_FAILED);
        assertThat(result.totalPriceEur()).isEqualTo("26.67 EUR");
    }

    @Test
    void getSummary_exchangeRateFails_returnsFallbackEurMessage() {
        when(invoiceService.generateInvoicePdf(5L))
                .thenReturn(CompletableFuture.completedFuture("invoice-5.pdf"));
        when(exchangeRateService.convertUahToEur(PRICE_UAH))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Exchange rate API is unavailable")));

        BookingSummaryDTO result = bookingSummaryService.getSummary(5L);

        assertThat(result.invoice()).isEqualTo("invoice-5.pdf");
        assertThat(result.totalPriceUah()).isEqualTo(PRICE_UAH);
        assertThat(result.totalPriceEur()).isEqualTo(BookingSummaryService.EUR_UNAVAILABLE);
    }

    @Test
    void getSummary_invoiceTooSlow_returnsPendingMessageAfterTimeout() {
        when(invoiceService.generateInvoicePdf(5L))
                .thenReturn(new CompletableFuture<>());
        when(exchangeRateService.convertUahToEur(PRICE_UAH))
                .thenReturn(CompletableFuture.completedFuture(new BigDecimal("26.67")));

        BookingSummaryDTO result = assertTimeout(Duration.ofSeconds(1),
                () -> bookingSummaryService.getSummary(5L));

        assertThat(result.invoice()).isEqualTo(BookingSummaryService.INVOICE_PENDING);
        assertThat(result.totalPriceEur()).isEqualTo("26.67 EUR");
    }

    @Test
    void getSummary_executorFull_throwsServiceOverloadedException() {
        when(invoiceService.generateInvoicePdf(5L))
                .thenThrow(new TaskRejectedException("Executor is full"));

        assertThatThrownBy(() -> bookingSummaryService.getSummary(5L))
                .isInstanceOf(ServiceOverloadedException.class)
                .hasMessage(BookingSummaryService.OVERLOADED)
                .hasCauseInstanceOf(TaskRejectedException.class);
        verifyNoInteractions(exchangeRateService);
    }


    private static BookingDTO booking(Long id) {
        BookingDTO dto = new BookingDTO();
        dto.setId(id);
        dto.setTotalPrice(PRICE_UAH);
        return dto;
    }
}