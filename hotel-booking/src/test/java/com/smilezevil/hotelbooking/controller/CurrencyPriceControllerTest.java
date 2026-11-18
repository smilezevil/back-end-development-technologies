package com.smilezevil.hotelbooking.controller;

import com.smilezevil.hotelbooking.dto.CurrencyPriceDTO;
import com.smilezevil.hotelbooking.dto.StayQuoteDTO;
import com.smilezevil.hotelbooking.exception.RateNotFoundException;
import com.smilezevil.hotelbooking.exception.RatesCircuitOpenException;
import com.smilezevil.hotelbooking.exception.RatesUnavailableException;
import com.smilezevil.hotelbooking.exception.ResourceNotFoundException;
import com.smilezevil.hotelbooking.service.CurrencyPriceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CurrencyPriceController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class CurrencyPriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrencyPriceService currencyPriceService;

    @Test
    void roomPrice_returns200WithConvertedAmount() throws Exception {
        when(currencyPriceService.roomPrice(1L, "EUR")).thenReturn(new CurrencyPriceDTO(
                1L, new BigDecimal("1350.00"), "EUR", new BigDecimal("30.00"), "http"));

        mockMvc.perform(get("/api/rooms/1/price").param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.amount").value(30.0))
                .andExpect(jsonPath("$.via").value("http"));
    }

    @Test
    void stayQuote_returns200WithTotal() throws Exception {
        when(currencyPriceService.stayQuote(1L, 3, "PLN")).thenReturn(new StayQuoteDTO(
                1L, 3, new BigDecimal("4050.00"), "PLN", new BigDecimal("10.40"), new BigDecimal("389.42"), "grpc"));

        mockMvc.perform(get("/api/rooms/1/quote").param("nights", "3").param("currency", "PLN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nights").value(3))
                .andExpect(jsonPath("$.total").value(389.42))
                .andExpect(jsonPath("$.via").value("grpc"));
    }

    @Test
    void bookingPrice_rateNotFound_returns404() throws Exception {
        when(currencyPriceService.bookingPrice(7L, "XYZ")).thenThrow(new RateNotFoundException("XYZ"));

        mockMvc.perform(get("/api/bookings/7/price").param("currency", "XYZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Курс для валюти XYZ не знайдено"));
    }

    @Test
    void roomPrice_ratesUnavailable_returns503() throws Exception {
        when(currencyPriceService.roomPrice(1L, "EUR")).thenThrow(new RatesUnavailableException(
                "Сервіс курсів валют недоступний (grpc): DEADLINE_EXCEEDED", null));

        mockMvc.perform(get("/api/rooms/1/price").param("currency", "EUR"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(containsString("DEADLINE_EXCEEDED")));
    }

    @Test
    void roomPrice_unknownRoom_returns404() throws Exception {
        when(currencyPriceService.roomPrice(99L, "EUR")).thenThrow(new ResourceNotFoundException("Кімнату не знайдено"));

        mockMvc.perform(get("/api/rooms/99/price").param("currency", "EUR"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Кімнату не знайдено"));
    }

    @Test
    void stayQuote_zeroNights_returns400() throws Exception {
        when(currencyPriceService.stayQuote(1L, 0, "EUR"))
                .thenThrow(new IllegalArgumentException("Кількість ночей має бути не менше 1"));

        mockMvc.perform(get("/api/rooms/1/quote").param("nights", "0").param("currency", "EUR"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void roomPrice_circuitOpen_returns503WithReason() throws Exception {
        when(currencyPriceService.roomPrice(1L, "EUR")).thenThrow(new RatesCircuitOpenException(null));

        mockMvc.perform(get("/api/rooms/1/price").param("currency", "EUR"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(containsString("circuit breaker")));
    }
}