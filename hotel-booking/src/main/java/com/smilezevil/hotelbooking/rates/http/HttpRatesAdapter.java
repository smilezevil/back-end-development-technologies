package com.smilezevil.hotelbooking.rates.http;

import com.smilezevil.common.dto.ConversionResponse;
import com.smilezevil.common.dto.ExchangeRateResponse;
import com.smilezevil.hotelbooking.exception.RateNotFoundException;
import com.smilezevil.hotelbooking.exception.RatesUnavailableException;
import com.smilezevil.hotelbooking.rates.ConvertedAmount;
import com.smilezevil.hotelbooking.rates.CurrencyConverter;
import com.smilezevil.hotelbooking.rates.ExchangeRate;
import com.smilezevil.hotelbooking.rates.RateLookup;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.function.Supplier;

public class HttpRatesAdapter implements RateLookup, CurrencyConverter {

    private static final String VIA = "http";

    private final ExchangeRateHttpClient client;

    public HttpRatesAdapter(ExchangeRateHttpClient client) {
        this.client = client;
    }

    @Override
    public ExchangeRate rateFor(String currency) {
        ExchangeRateResponse response = call(currency, () -> client.rate(currency));
        return new ExchangeRate(response.currency(), response.uahPerUnit(), VIA);
    }

    @Override
    public ConvertedAmount convert(BigDecimal amountUah, String currency) {
        ConversionResponse response = call(currency, () -> client.convert(currency, amountUah));
        return new ConvertedAmount(response.amountUah(), response.currency(), response.amount(), VIA);
    }

    private <T> T call(String currency, Supplier<T> request) {
        try {
            return request.get();
        } catch (HttpClientErrorException.NotFound e) {
            throw new RateNotFoundException(currency);
        } catch (RestClientException e) {
            throw new RatesUnavailableException("Сервіс курсів валют недоступний (http): "
                    + NestedExceptionUtils.getMostSpecificCause(e).getClass().getSimpleName(), e);
        }
    }
}