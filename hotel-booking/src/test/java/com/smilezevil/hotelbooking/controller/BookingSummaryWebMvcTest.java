package com.smilezevil.hotelbooking.controller;

import com.smilezevil.hotelbooking.dto.BookingSummaryDTO;
import com.smilezevil.hotelbooking.exception.ServiceOverloadedException;
import com.smilezevil.hotelbooking.service.BookingService;
import com.smilezevil.hotelbooking.service.BookingSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = BookingController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class BookingSummaryWebMvcTest {

    private static final String OVERLOADED = "Сервер зараз перевантажений, спробуйте пізніше";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private BookingSummaryService bookingSummaryService;


    @Test
    void getSummary_tasksFinished_returns200WithSummary() throws Exception {
        when(bookingSummaryService.getSummary(5L)).thenReturn(
                new BookingSummaryDTO(5L, "invoice-5.pdf", new BigDecimal("1200.00"), "26.67 EUR"));

        mockMvc.perform(get("/api/bookings/5/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(5))
                .andExpect(jsonPath("$.invoice").value("invoice-5.pdf"))
                .andExpect(jsonPath("$.totalPriceEur").value("26.67 EUR"));
    }

    @Test
    void getSummary_serviceOverloaded_returns503WithRetryAfter() throws Exception {
        when(bookingSummaryService.getSummary(5L)).thenThrow(
                new ServiceOverloadedException(OVERLOADED, new TaskRejectedException("Executor is full")));

        mockMvc.perform(get("/api/bookings/5/summary"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "5"))
                .andExpect(jsonPath("$.message").value(OVERLOADED));
    }
}