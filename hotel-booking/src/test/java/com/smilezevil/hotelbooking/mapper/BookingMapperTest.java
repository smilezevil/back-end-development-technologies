package com.smilezevil.hotelbooking.mapper;

import com.smilezevil.hotelbooking.dto.BookingDTO;
import com.smilezevil.hotelbooking.entity.Booking;
import com.smilezevil.hotelbooking.entity.Guest;
import com.smilezevil.hotelbooking.entity.Room;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BookingMapperTest {

    private final BookingMapper bookingMapper = Mappers.getMapper(BookingMapper.class);

    // ===== toDto =====

    @Test
    void toDto_bookingWithRoomAndGuest_mapsIdsAndFields() {
        // Arrange
        Room room = new Room();
        room.setId(10L);
        Guest guest = new Guest();
        guest.setId(5L);

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setRoom(room);
        booking.setGuest(guest);
        booking.setCheckInDate(LocalDate.of(2026, 10, 10));
        booking.setCheckOutDate(LocalDate.of(2026, 10, 15));
        booking.setStatus("CONFIRMED");
        booking.setTotalPrice(new BigDecimal("6750.00"));

        // Act
        BookingDTO dto = bookingMapper.toDto(booking);

        // Assert
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getRoomId()).isEqualTo(10L);
        assertThat(dto.getGuestId()).isEqualTo(5L);
        assertThat(dto.getCheckInDate()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(dto.getCheckOutDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(dto.getStatus()).isEqualTo("CONFIRMED");
        assertThat(dto.getTotalPrice()).isEqualTo(new BigDecimal("6750.00"));
    }

    @Test
    void toDto_bookingWithoutRoomAndGuest_returnsNullIds() {
        // Arrange
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus("PENDING");

        // Act
        BookingDTO dto = bookingMapper.toDto(booking);

        // Assert
        assertThat(dto.getRoomId()).isNull();
        assertThat(dto.getGuestId()).isNull();
        assertThat(dto.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void toDto_nullBooking_returnsNull() {
        // Act
        BookingDTO dto = bookingMapper.toDto(null);

        // Assert
        assertThat(dto).isNull();
    }

    // ===== toEntity =====

    @Test
    void toEntity_filledDto_copiesFieldsButIgnoresRoomAndGuest() {
        // Arrange
        BookingDTO dto = new BookingDTO();
        dto.setRoomId(10L);
        dto.setGuestId(5L);
        dto.setCheckInDate(LocalDate.of(2026, 11, 1));
        dto.setCheckOutDate(LocalDate.of(2026, 11, 3));
        dto.setStatus("PENDING");
        dto.setTotalPrice(new BigDecimal("7000.00"));

        // Act
        Booking booking = bookingMapper.toEntity(dto);

        // Assert
        assertThat(booking.getRoom()).isNull();    // кімнату ставить сервіс
        assertThat(booking.getGuest()).isNull();   // гостя ставить сервіс
        assertThat(booking.getCheckInDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(booking.getStatus()).isEqualTo("PENDING");
        assertThat(booking.getTotalPrice()).isEqualTo(new BigDecimal("7000.00"));
    }

    @Test
    void toEntity_nullDto_returnsNull() {
        // Act
        Booking booking = bookingMapper.toEntity(null);

        // Assert
        assertThat(booking).isNull();
    }

    // ===== updateEntityFromDto =====

    @Test
    void updateEntityFromDto_newValues_updatesFieldsButKeepsIdRoomAndGuest() {
        // Arrange
        Room room = new Room();
        room.setId(10L);
        Guest guest = new Guest();
        guest.setId(5L);

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setRoom(room);
        booking.setGuest(guest);
        booking.setStatus("PENDING");

        BookingDTO changes = new BookingDTO();
        changes.setId(999L);          // спробуємо підмінити id
        changes.setRoomId(20L);       // і кімнату
        changes.setGuestId(30L);      // і гостя
        changes.setStatus("CONFIRMED");

        // Act
        bookingMapper.updateEntityFromDto(changes, booking);

        // Assert
        assertThat(booking.getId()).isEqualTo(1L);        // id НЕ змінився
        assertThat(booking.getRoom()).isSameAs(room);     // кімната НЕ змінилась
        assertThat(booking.getGuest()).isSameAs(guest);   // гість НЕ змінився
        assertThat(booking.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void updateEntityFromDto_nullDto_leavesBookingUnchanged() {
        // Arrange
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus("PENDING");

        // Act
        bookingMapper.updateEntityFromDto(null, booking);

        // Assert
        assertThat(booking.getId()).isEqualTo(1L);
        assertThat(booking.getStatus()).isEqualTo("PENDING");
    }
}