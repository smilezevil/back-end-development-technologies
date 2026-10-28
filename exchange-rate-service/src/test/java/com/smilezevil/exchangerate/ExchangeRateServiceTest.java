package com.smilezevil.exchangerate;

import com.smilezevil.common.dto.ConversionResponse;
import com.smilezevil.common.dto.ExchangeRateResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeRateServiceTest {

    private final ExchangeRateService exchangeRateService = new ExchangeRateService();


    @Test
    void rateFor_lowerCaseCodeWithSpaces_returnsNormalizedCurrencyAndRate() {
        ExchangeRateResponse response = exchangeRateService.rateFor(" eur ");

        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.uahPerUnit()).isEqualByComparingTo("45.00");
    }

    @Test
    void convertFromUah_eur_dividesAndRoundsToTwoDecimals() {
        ConversionResponse response = exchangeRateService.convertFromUah(new BigDecimal("1200"), "EUR");

        assertThat(response.amount()).isEqualByComparingTo("26.67");
        assertThat(response.amountUah()).isEqualByComparingTo("1200");
    }

    @Test
    void rateFor_unknownCurrency_throwsWithSupportedCurrencies() {
        assertThatThrownBy(() -> exchangeRateService.rateFor("XYZ"))
                .isInstanceOf(UnknownCurrencyException.class)
                .hasMessage("Unknown currency 'XYZ', supported: [EUR, PLN, USD]");
    }

    @Test
    void convertFromUah_negativeAmount_throwsIllegalArgument() {
        assertThatThrownBy(() -> exchangeRateService.convertFromUah(new BigDecimal("-1"), "EUR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount in UAH must not be negative");
    }
}