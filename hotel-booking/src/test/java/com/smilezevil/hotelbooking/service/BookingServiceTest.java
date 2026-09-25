package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.dto.BookingDTO;
import com.smilezevil.hotelbooking.entity.Booking;
import com.smilezevil.hotelbooking.entity.Guest;
import com.smilezevil.hotelbooking.entity.Room;
import com.smilezevil.hotelbooking.mapper.BookingMapper;
import com.smilezevil.hotelbooking.repository.BookingRepository;
import com.smilezevil.hotelbooking.repository.GuestRepository;
import com.smilezevil.hotelbooking.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingService bookingService;

    @Captor
    private ArgumentCaptor<Booking> bookingCaptor;


    @Test
    void create_existingRoomAndGuest_linksThemAndSavesBooking() {
        Room room = room(1L);
        Guest guest = guest(2L);
        BookingDTO inputDto = bookingDto(null, 1L, 2L);
        Booking booking = booking(null, null, null);   // мапер room/guest не ставить
        Booking savedBooking = booking(100L, room, guest);

        when(bookingMapper.toEntity(inputDto)).thenReturn(booking);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(guestRepository.findById(2L)).thenReturn(Optional.of(guest));
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(bookingMapper.toDto(savedBooking)).thenReturn(bookingDto(100L, 1L, 2L));

        BookingDTO result = bookingService.create(inputDto);

        verify(bookingRepository).save(bookingCaptor.capture());
        Booking bookingPassedToSave = bookingCaptor.getValue();
        assertThat(bookingPassedToSave.getRoom()).isSameAs(room);
        assertThat(bookingPassedToSave.getGuest()).isSameAs(guest);
        assertThat(bookingPassedToSave.getCheckInDate()).isEqualTo(LocalDate.of(2026, 10, 10));

        assertThat(result.getId()).isEqualTo(100L);
    }

    @Test
    void create_nonExistingRoom_throwsAndDoesNotSave() {
        BookingDTO inputDto = bookingDto(null, 99L, 2L);

        when(bookingMapper.toEntity(inputDto)).thenReturn(booking(null, null, null));
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(inputDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Кімнату не знайдено");
        verifyNoInteractions(guestRepository);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void create_nonExistingGuest_throwsAndDoesNotSave() {
        BookingDTO inputDto = bookingDto(null, 1L, 99L);

        when(bookingMapper.toEntity(inputDto)).thenReturn(booking(null, null, null));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room(1L)));
        when(guestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(inputDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Гостя не знайдено");
        verify(bookingRepository, never()).save(any());
    }


    @Test
    void findAll_bookingsExist_returnsAllBookings() {
        Booking first = booking(100L, room(1L), guest(2L));
        Booking second = booking(101L, room(1L), guest(3L));

        when(bookingRepository.findAll()).thenReturn(List.of(first, second));
        when(bookingMapper.toDto(first)).thenReturn(bookingDto(100L, 1L, 2L));
        when(bookingMapper.toDto(second)).thenReturn(bookingDto(101L, 1L, 3L));

        List<BookingDTO> result = bookingService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(100L);
        assertThat(result.get(1).getGuestId()).isEqualTo(3L);
    }

    @Test
    void findAll_noBookings_returnsEmptyList() {
        when(bookingRepository.findAll()).thenReturn(List.of());

        List<BookingDTO> result = bookingService.findAll();

        assertThat(result).isEmpty();
        verifyNoInteractions(bookingMapper);
    }


    @Test
    void findById_existingId_returnsBooking() {
        Booking booking = booking(100L, room(1L), guest(2L));

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingMapper.toDto(booking)).thenReturn(bookingDto(100L, 1L, 2L));

        BookingDTO result = bookingService.findById(100L);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void findById_nonExistingId_throwsNotFoundException() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.findById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Бронювання не знайдено");
    }


    @Test
    void update_existingBookingRoomAndGuest_movesToNewRoomAndSaves() {
        Room oldRoom = room(1L);
        Room newRoom = room(3L);
        Guest guest = guest(2L);
        Booking existing = booking(100L, oldRoom, guest);
        BookingDTO changes = bookingDto(null, 3L, 2L);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(roomRepository.findById(3L)).thenReturn(Optional.of(newRoom));
        when(guestRepository.findById(2L)).thenReturn(Optional.of(guest));
        when(bookingRepository.save(any(Booking.class))).thenReturn(existing);
        when(bookingMapper.toDto(existing)).thenReturn(bookingDto(100L, 3L, 2L));

        BookingDTO result = bookingService.update(100L, changes);

        verify(bookingMapper).updateEntityFromDto(changes, existing);
        verify(bookingRepository).save(bookingCaptor.capture());
        assertThat(bookingCaptor.getValue().getRoom()).isSameAs(newRoom);
        assertThat(bookingCaptor.getValue().getGuest()).isSameAs(guest);
        assertThat(result.getRoomId()).isEqualTo(3L);
    }

    @Test
    void update_nonExistingBooking_throwsAndDoesNotSave() {
        BookingDTO changes = bookingDto(null, 1L, 2L);
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.update(999L, changes))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Бронювання не знайдено");
        verifyNoInteractions(roomRepository, guestRepository);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void update_nonExistingRoom_throwsAndDoesNotSave() {
        Booking existing = booking(100L, room(1L), guest(2L));
        BookingDTO changes = bookingDto(null, 99L, 2L);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.update(100L, changes))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Кімнату не знайдено");
        verifyNoInteractions(guestRepository);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void update_nonExistingGuest_throwsAndDoesNotSave() {
        Booking existing = booking(100L, room(1L), guest(2L));
        BookingDTO changes = bookingDto(null, 1L, 99L);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room(1L)));
        when(guestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.update(100L, changes))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Гостя не знайдено");
        verify(bookingRepository, never()).save(any());
    }


    @Test
    void delete_existingId_callsRepositoryDelete() {
        bookingService.delete(100L);

        verify(bookingRepository).deleteById(100L);
    }

    @Test
    void delete_databaseError_throwsException() {
        doThrow(new RuntimeException("DB is down")).when(bookingRepository).deleteById(100L);

        assertThatThrownBy(() -> bookingService.delete(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB is down");
    }


    private static Room room(Long id) {
        Room room = new Room();
        room.setId(id);
        room.setRoomNumber("10" + id);
        return room;
    }

    private static Guest guest(Long id) {
        Guest guest = new Guest();
        guest.setId(id);
        guest.setEmail("guest" + id + "@gmail.com");
        return guest;
    }

    private static Booking booking(Long id, Room room, Guest guest) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setRoom(room);
        booking.setGuest(guest);
        booking.setCheckInDate(LocalDate.of(2026, 10, 10));
        booking.setCheckOutDate(LocalDate.of(2026, 10, 15));
        booking.setStatus("CONFIRMED");
        booking.setTotalPrice(new BigDecimal("6750.00"));
        return booking;
    }

    private static BookingDTO bookingDto(Long id, Long roomId, Long guestId) {
        BookingDTO dto = new BookingDTO();
        dto.setId(id);
        dto.setRoomId(roomId);
        dto.setGuestId(guestId);
        dto.setCheckInDate(LocalDate.of(2026, 10, 10));
        dto.setCheckOutDate(LocalDate.of(2026, 10, 15));
        dto.setStatus("CONFIRMED");
        dto.setTotalPrice(new BigDecimal("6750.00"));
        return dto;
    }
}