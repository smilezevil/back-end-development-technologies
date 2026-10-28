package com.smilezevil.exchangerate;

import com.smilezevil.common.dto.ConversionResponse;
import com.smilezevil.common.dto.ExchangeRateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/rates")
public class ExchangeRateController {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateController.class);

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping("/{currency}")
    public ExchangeRateResponse rate(@PathVariable String currency) {
        log.info("Rate requested for {}", currency);
        return exchangeRateService.rateFor(currency);
    }

    @GetMapping("/{currency}/convert")
    public ConversionResponse convert(@PathVariable String currency, @RequestParam BigDecimal amountUah) {
        log.info("Conversion requested: {} UAH -> {}", amountUah, currency);
        return exchangeRateService.convertFromUah(amountUah, currency);
    }
}