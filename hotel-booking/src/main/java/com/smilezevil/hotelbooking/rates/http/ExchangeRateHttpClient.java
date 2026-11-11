package com.smilezevil.hotelbooking.rates.http;

import com.smilezevil.common.dto.ConversionResponse;
import com.smilezevil.common.dto.ExchangeRateResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.math.BigDecimal;

@HttpExchange("/rates")
public interface ExchangeRateHttpClient {

    @GetExchange("/{currency}")
    ExchangeRateResponse rate(@PathVariable("currency") String currency);

    @GetExchange("/{currency}/convert")
    ConversionResponse convert(@PathVariable("currency") String currency,
                               @RequestParam("amountUah") BigDecimal amountUah);
}