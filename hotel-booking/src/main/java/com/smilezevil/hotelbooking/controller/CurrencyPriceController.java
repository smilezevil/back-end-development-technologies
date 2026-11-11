package com.smilezevil.hotelbooking.controller;

import com.smilezevil.hotelbooking.dto.CurrencyPriceDTO;
import com.smilezevil.hotelbooking.dto.StayQuoteDTO;
import com.smilezevil.hotelbooking.service.CurrencyPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CurrencyPriceController {

    private final CurrencyPriceService currencyPriceService;

    @GetMapping("/rooms/{id}/price")
    public CurrencyPriceDTO roomPrice(@PathVariable Long id, @RequestParam String currency) {
        return currencyPriceService.roomPrice(id, currency);
    }

    @GetMapping("/bookings/{id}/price")
    public CurrencyPriceDTO bookingPrice(@PathVariable Long id, @RequestParam String currency) {
        return currencyPriceService.bookingPrice(id, currency);
    }

    @GetMapping("/rooms/{id}/quote")
    public StayQuoteDTO stayQuote(@PathVariable Long id,
                                  @RequestParam int nights,
                                  @RequestParam String currency) {
        return currencyPriceService.stayQuote(id, nights, currency);
    }
}