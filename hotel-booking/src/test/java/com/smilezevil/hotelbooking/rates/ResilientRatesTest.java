package com.smilezevil.hotelbooking.rates;

import com.smilezevil.hotelbooking.exception.RateNotFoundException;
import com.smilezevil.hotelbooking.exception.RatesCircuitOpenException;
import com.smilezevil.hotelbooking.exception.RatesUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = {ResilientRates.class, ResilienceEventLogger.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "resilience4j.retry.instances.rates.wait-duration=10ms",
                "resilience4j.circuitbreaker.instances.rates.wait-duration-in-open-state=300ms"
        })
@ImportAutoConfiguration({
        AopAutoConfiguration.class,
        RetryAutoConfiguration.class,
        CircuitBreakerAutoConfiguration.class
})
@ExtendWith(OutputCaptureExtension.class)
class ResilientRatesTest {

    private static final BigDecimal ROOM_PRICE = new BigDecimal("1350.00");

    @MockBean
    private RatesGateway gateway;

    @Autowired
    private ResilientRates rates;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreaker breaker;

    @BeforeEach
    void resetCircuit() {
        breaker = circuitBreakerRegistry.circuitBreaker("rates");
        breaker.reset();
    }

    @Test
    void notFound_isNotRetried_andDoesNotOpenCircuit() {
        when(gateway.rateFor("XYZ")).thenThrow(new RateNotFoundException("XYZ"));

        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> rates.rateFor("XYZ")).isInstanceOf(RateNotFoundException.class);
        }

        verify(gateway, times(10)).rateFor("XYZ");
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void temporaryFailure_isRetried_untilSuccess(CapturedOutput output) {
        ConvertedAmount converted = new ConvertedAmount(ROOM_PRICE, "EUR", new BigDecimal("30.00"), "http");
        RatesUnavailableException down = new RatesUnavailableException("down", null);
        when(gateway.convert(ROOM_PRICE, "EUR")).thenThrow(down, down).thenReturn(converted);

        assertThat(rates.convert(ROOM_PRICE, "EUR")).isEqualTo(converted);

        verify(gateway, times(3)).convert(ROOM_PRICE, "EUR");
        assertThat(output)
                .contains("RETRY rates: call failed with RatesUnavailableException, retry #1")
                .contains("RETRY rates: call failed with RatesUnavailableException, retry #2")
                .contains("RETRY rates: succeeded after 2 retries");
    }

    @Test
    void repeatedFailures_openCircuit_andNextCallIsRejectedWithoutCallingGateway(CapturedOutput output) {
        when(gateway.rateFor("EUR")).thenThrow(new RatesUnavailableException("down", null));

        assertThatThrownBy(() -> rates.rateFor("EUR")).isInstanceOf(RatesUnavailableException.class);
        assertThatThrownBy(() -> rates.rateFor("EUR")).isInstanceOf(RatesUnavailableException.class);

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        verify(gateway, times(6)).rateFor("EUR");

        assertThatThrownBy(() -> rates.rateFor("EUR")).isInstanceOf(RatesCircuitOpenException.class);

        verify(gateway, times(6)).rateFor("EUR");
        assertThat(output)
                .contains("RETRY rates: gave up after 3 attempts")
                .contains("CIRCUIT rates: CLOSED -> OPEN")
                .contains("CIRCUIT OPEN: call to exchange-rate-service rejected");
    }

    @Test
    void afterWaitDuration_successfulProbeCalls_closeCircuitAgain(CapturedOutput output) throws InterruptedException {
        ExchangeRate eur = new ExchangeRate("EUR", new BigDecimal("45.00"), "http");
        RatesUnavailableException down = new RatesUnavailableException("down", null);
        when(gateway.rateFor("EUR")).thenThrow(down, down, down, down, down, down).thenReturn(eur);

        assertThatThrownBy(() -> rates.rateFor("EUR")).isInstanceOf(RatesUnavailableException.class);
        assertThatThrownBy(() -> rates.rateFor("EUR")).isInstanceOf(RatesUnavailableException.class);
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        Thread.sleep(400);

        assertThat(rates.rateFor("EUR")).isEqualTo(eur);
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertThat(rates.rateFor("EUR")).isEqualTo(eur);
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(output)
                .contains("CIRCUIT rates: OPEN -> HALF_OPEN")
                .contains("CIRCUIT rates: HALF_OPEN -> CLOSED");
    }
}