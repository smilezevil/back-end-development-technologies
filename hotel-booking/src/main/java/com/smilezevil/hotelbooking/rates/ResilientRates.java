package com.smilezevil.hotelbooking.rates;

import com.smilezevil.hotelbooking.exception.RatesCircuitOpenException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Primary
@Component
public class ResilientRates implements RateLookup, CurrencyConverter {

    private static final Logger log = LoggerFactory.getLogger(ResilientRates.class);
    private static final String RATES = "rates";

    private final RatesGateway gateway;

    public ResilientRates(RatesGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    @Retry(name = RATES)
    @CircuitBreaker(name = RATES, fallbackMethod = "rateCircuitOpen")
    public ExchangeRate rateFor(String currency) {
        return gateway.rateFor(currency);
    }

    @Override
    @Retry(name = RATES)
    @CircuitBreaker(name = RATES, fallbackMethod = "convertCircuitOpen")
    public ConvertedAmount convert(BigDecimal amountUah, String currency) {
        return gateway.convert(amountUah, currency);
    }

    public ExchangeRate rateCircuitOpen(String currency, CallNotPermittedException e) {
        throw circuitOpen(e);
    }

    public ConvertedAmount convertCircuitOpen(BigDecimal amountUah, String currency, CallNotPermittedException e) {
        throw circuitOpen(e);
    }

    private RatesCircuitOpenException circuitOpen(CallNotPermittedException e) {
        log.warn("CIRCUIT OPEN: call to exchange-rate-service rejected without calling it");
        return new RatesCircuitOpenException(e);
    }
}