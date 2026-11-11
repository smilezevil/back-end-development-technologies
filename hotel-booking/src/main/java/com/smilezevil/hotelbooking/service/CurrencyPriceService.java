package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.dto.CurrencyPriceDTO;
import com.smilezevil.hotelbooking.dto.StayQuoteDTO;
import com.smilezevil.hotelbooking.entity.Booking;
import com.smilezevil.hotelbooking.entity.Room;
import com.smilezevil.hotelbooking.exception.ResourceNotFoundException;
import com.smilezevil.hotelbooking.rates.ConvertedAmount;
import com.smilezevil.hotelbooking.rates.CurrencyConverter;
import com.smilezevil.hotelbooking.rates.ExchangeRate;
import com.smilezevil.hotelbooking.rates.RateLookup;
import com.smilezevil.hotelbooking.repository.BookingRepository;
import com.smilezevil.hotelbooking.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CurrencyPriceService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final CurrencyConverter currencyConverter;
    private final RateLookup rateLookup;

    public CurrencyPriceDTO roomPrice(Long roomId, String currency) {
        Room room = findRoom(roomId);
        ConvertedAmount converted = currencyConverter.convert(room.getPricePerNight(), currency);
        return toDto(roomId, converted);
    }

    public CurrencyPriceDTO bookingPrice(Long bookingId, String currency) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Бронювання не знайдено"));
        ConvertedAmount converted = currencyConverter.convert(booking.getTotalPrice(), currency);
        return toDto(bookingId, converted);
    }

    public StayQuoteDTO stayQuote(Long roomId, int nights, String currency) {
        if (nights < 1) {
            throw new IllegalArgumentException("Кількість ночей має бути не менше 1");
        }
        Room room = findRoom(roomId);
        ExchangeRate rate = rateLookup.rateFor(currency);
        BigDecimal totalUah = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
        BigDecimal total = totalUah.divide(rate.uahPerUnit(), 2, RoundingMode.HALF_UP);
        return new StayQuoteDTO(roomId, nights, totalUah, rate.currency(), rate.uahPerUnit(), total, rate.via());
    }

    private Room findRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Кімнату не знайдено"));
    }

    private CurrencyPriceDTO toDto(Long id, ConvertedAmount converted) {
        return new CurrencyPriceDTO(id, converted.amountUah(), converted.currency(), converted.amount(), converted.via());
    }
}