package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.dto.CurrencyPriceDTO;
import com.smilezevil.hotelbooking.dto.StayQuoteDTO;
import com.smilezevil.hotelbooking.entity.Booking;
import com.smilezevil.hotelbooking.entity.Room;
import com.smilezevil.hotelbooking.exception.RateNotFoundException;
import com.smilezevil.hotelbooking.exception.ResourceNotFoundException;
import com.smilezevil.hotelbooking.rates.ConvertedAmount;
import com.smilezevil.hotelbooking.rates.CurrencyConverter;
import com.smilezevil.hotelbooking.rates.ExchangeRate;
import com.smilezevil.hotelbooking.rates.RateLookup;
import com.smilezevil.hotelbooking.repository.BookingRepository;
import com.smilezevil.hotelbooking.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyPriceServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CurrencyConverter currencyConverter;

    @Mock
    private RateLookup rateLookup;

    @InjectMocks
    private CurrencyPriceService service;

    @Test
    void roomPrice_convertsPricePerNightThroughPort() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room("1350.00")));
        when(currencyConverter.convert(new BigDecimal("1350.00"), "EUR"))
                .thenReturn(new ConvertedAmount(new BigDecimal("1350.00"), "EUR", new BigDecimal("30.00"), "grpc"));

        CurrencyPriceDTO price = service.roomPrice(1L, "EUR");

        assertThat(price.id()).isEqualTo(1L);
        assertThat(price.amount()).isEqualByComparingTo("30.00");
        assertThat(price.currency()).isEqualTo("EUR");
        assertThat(price.via()).isEqualTo("grpc");
        verifyNoInteractions(rateLookup);
    }

    @Test
    void bookingPrice_convertsTotalPriceThroughPort() {
        Booking booking = new Booking();
        booking.setTotalPrice(new BigDecimal("5400.00"));
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
        when(currencyConverter.convert(new BigDecimal("5400.00"), "USD"))
                .thenReturn(new ConvertedAmount(new BigDecimal("5400.00"), "USD", new BigDecimal("130.12"), "http"));

        CurrencyPriceDTO price = service.bookingPrice(7L, "USD");

        assertThat(price.id()).isEqualTo(7L);
        assertThat(price.amountUah()).isEqualByComparingTo("5400.00");
        assertThat(price.amount()).isEqualByComparingTo("130.12");
    }

    @Test
    void stayQuote_multipliesNightsAndDividesByRate() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room("1350.00")));
        when(rateLookup.rateFor("PLN")).thenReturn(new ExchangeRate("PLN", new BigDecimal("10.40"), "http"));

        StayQuoteDTO quote = service.stayQuote(1L, 3, "PLN");

        assertThat(quote.totalUah()).isEqualByComparingTo("4050.00");
        assertThat(quote.uahPerUnit()).isEqualByComparingTo("10.40");
        assertThat(quote.total()).isEqualByComparingTo("389.42");
        assertThat(quote.via()).isEqualTo("http");
        verifyNoInteractions(currencyConverter);
    }

    @Test
    void roomPrice_unknownRoom_throwsNotFoundAndDoesNotCallRates() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.roomPrice(99L, "EUR"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Кімнату не знайдено");
        verifyNoInteractions(currencyConverter, rateLookup);
    }

    @Test
    void stayQuote_zeroNights_throwsIllegalArgumentAndDoesNothingElse() {
        assertThatThrownBy(() -> service.stayQuote(1L, 0, "EUR"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(roomRepository, rateLookup);
    }

    @Test
    void roomPrice_rateNotFoundFromPort_isPassedUnchanged() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room("1350.00")));
        when(currencyConverter.convert(new BigDecimal("1350.00"), "XYZ"))
                .thenThrow(new RateNotFoundException("XYZ"));

        assertThatThrownBy(() -> service.roomPrice(1L, "XYZ"))
                .isInstanceOf(RateNotFoundException.class);
        verify(currencyConverter).convert(new BigDecimal("1350.00"), "XYZ");
    }

    private static Room room(String pricePerNight) {
        Room room = new Room();
        room.setPricePerNight(new BigDecimal(pricePerNight));
        return room;
    }
}