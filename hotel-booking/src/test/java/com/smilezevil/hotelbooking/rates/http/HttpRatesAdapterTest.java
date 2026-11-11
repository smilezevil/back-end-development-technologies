package com.smilezevil.hotelbooking.rates.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.smilezevil.hotelbooking.exception.RateNotFoundException;
import com.smilezevil.hotelbooking.exception.RatesUnavailableException;
import com.smilezevil.hotelbooking.rates.ConvertedAmount;
import com.smilezevil.hotelbooking.rates.ExchangeRate;
import com.smilezevil.hotelbooking.rates.RatesProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpRatesAdapterTest {

    private WireMockServer exchangeRateService;
    private HttpRatesAdapter adapter;

    @BeforeEach
    void startFakeExchangeRateService() {
        exchangeRateService = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        exchangeRateService.start();
        RatesProperties.Rest rest = new RatesProperties.Rest(
                "http://localhost:" + exchangeRateService.port(),
                Duration.ofMillis(500),
                Duration.ofMillis(500));
        adapter = new HttpRatesAdapter(HttpRatesConfig.createClient(rest));
    }

    @AfterEach
    void stopFakeExchangeRateService() {
        exchangeRateService.stop();
    }

    @Test
    void rateFor_knownCurrency_returnsRateViaHttp() {
        exchangeRateService.stubFor(get(urlPathEqualTo("/rates/EUR"))
                .willReturn(okJson("{\"currency\":\"EUR\",\"uahPerUnit\":45.00}")));

        ExchangeRate rate = adapter.rateFor("EUR");

        assertThat(rate.currency()).isEqualTo("EUR");
        assertThat(rate.uahPerUnit()).isEqualByComparingTo("45.00");
        assertThat(rate.via()).isEqualTo("http");
    }

    @Test
    void convert_sendsAmountAsQueryParam_returnsConvertedAmount() {
        exchangeRateService.stubFor(get(urlPathEqualTo("/rates/USD/convert"))
                .withQueryParam("amountUah", equalTo("1200.00"))
                .willReturn(okJson("{\"amountUah\":1200.00,\"currency\":\"USD\",\"amount\":28.92}")));

        ConvertedAmount converted = adapter.convert(new BigDecimal("1200.00"), "USD");

        assertThat(converted.currency()).isEqualTo("USD");
        assertThat(converted.amount()).isEqualByComparingTo("28.92");
        assertThat(converted.via()).isEqualTo("http");
    }

    @Test
    void notFoundResponse_becomesRateNotFound() {
        exchangeRateService.stubFor(get(urlPathEqualTo("/rates/XYZ"))
                .willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> adapter.rateFor("XYZ"))
                .isInstanceOf(RateNotFoundException.class);
    }

    @Test
    void serverError_becomesRatesUnavailable() {
        exchangeRateService.stubFor(get(urlPathEqualTo("/rates/EUR"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> adapter.rateFor("EUR"))
                .isInstanceOf(RatesUnavailableException.class);
    }

    @Test
    void slowAnswer_isCutByReadTimeout() {
        exchangeRateService.stubFor(get(urlPathEqualTo("/rates/EUR"))
                .willReturn(okJson("{\"currency\":\"EUR\",\"uahPerUnit\":45.00}").withFixedDelay(2000)));

        long start = System.currentTimeMillis();

        assertThatThrownBy(() -> adapter.rateFor("EUR"))
                .isInstanceOf(RatesUnavailableException.class);
        assertThat(System.currentTimeMillis() - start).isLessThan(1500);
    }

    @Test
    void serviceDown_becomesRatesUnavailable() {
        exchangeRateService.stop();

        assertThatThrownBy(() -> adapter.rateFor("EUR"))
                .isInstanceOf(RatesUnavailableException.class);
    }
}