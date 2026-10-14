package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.dto.BookingDTO;
import com.smilezevil.hotelbooking.dto.BookingSummaryDTO;
import com.smilezevil.hotelbooking.exception.ServiceOverloadedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class BookingSummaryService {

    static final String INVOICE_PENDING = "рахунок ще генерується, спробуйте пізніше";
    static final String INVOICE_FAILED = "не вдалося згенерувати рахунок";
    static final String EUR_UNAVAILABLE = "курс тимчасово недоступний";
    static final String OVERLOADED = "Сервер зараз перевантажений, спробуйте пізніше";

    private final BookingService bookingService;
    private final InvoiceService invoiceService;
    private final ExchangeRateService exchangeRateService;
    private final long timeoutMs;

    public BookingSummaryService(BookingService bookingService,
                                 InvoiceService invoiceService,
                                 ExchangeRateService exchangeRateService,
                                 @Value("${booking.summary.timeout-ms:3000}") long timeoutMs) {
        this.bookingService = bookingService;
        this.invoiceService = invoiceService;
        this.exchangeRateService = exchangeRateService;
        this.timeoutMs = timeoutMs;
    }

    public BookingSummaryDTO getSummary(Long bookingId) {
        BookingDTO booking = bookingService.findById(bookingId);

        CompletableFuture<String> invoiceFuture;
        CompletableFuture<BigDecimal> eurFuture;
        try {
            invoiceFuture = invoiceService.generateInvoicePdf(bookingId);
            eurFuture = exchangeRateService.convertUahToEur(booking.getTotalPrice());
        } catch (TaskRejectedException ex) {
            log.warn("Async executor is full, summary for booking {} rejected", bookingId);
            throw new ServiceOverloadedException(OVERLOADED, ex);
        }

        CompletableFuture<String> invoice = invoiceFuture
                .exceptionally(ex -> {
                    log.warn("Invoice generation failed for booking {}: {}", bookingId, ex.getMessage());
                    return INVOICE_FAILED;
                })
                .completeOnTimeout(INVOICE_PENDING, timeoutMs, TimeUnit.MILLISECONDS);

        CompletableFuture<String> priceEur = eurFuture
                .thenApply(eur -> eur.toPlainString() + " EUR")
                .exceptionally(ex -> {
                    log.warn("Exchange rate unavailable for booking {}: {}", bookingId, ex.getMessage());
                    return EUR_UNAVAILABLE;
                })
                .completeOnTimeout(EUR_UNAVAILABLE, timeoutMs, TimeUnit.MILLISECONDS);

        return invoice
                .thenCombine(priceEur, (invoiceFile, eur) ->
                        new BookingSummaryDTO(bookingId, invoiceFile, booking.getTotalPrice(), eur))
                .join();
    }
}