package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.dto.BookingDTO;
import com.smilezevil.hotelbooking.entity.Booking;
import com.smilezevil.hotelbooking.entity.Guest;
import com.smilezevil.hotelbooking.entity.Room;
import com.smilezevil.hotelbooking.mapper.BookingMapper;
import com.smilezevil.hotelbooking.repository.BookingRepository;
import com.smilezevil.hotelbooking.repository.GuestRepository;
import com.smilezevil.hotelbooking.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final BookingMapper bookingMapper;

    public BookingDTO create(BookingDTO bookingDTO) {
        Booking booking = bookingMapper.toEntity(bookingDTO);
        Room room = roomRepository.findById(bookingDTO.getRoomId()).orElseThrow(() -> new RuntimeException("Кімнату не знайдено"));
        Guest guest = guestRepository.findById(bookingDTO.getGuestId()).orElseThrow(() -> new RuntimeException("Гостя не знайдено"));

        booking.setRoom(room);
        booking.setGuest(guest);
        return bookingMapper.toDto(bookingRepository.save(booking));
    }

    public List<BookingDTO> findAll() {
        return bookingRepository.findAll().stream().map(bookingMapper::toDto).collect(Collectors.toList());
    }

    public BookingDTO findById(Long id) {
        return bookingRepository.findById(id).map(bookingMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Бронювання не знайдено"));
    }

    public BookingDTO update(Long id, BookingDTO bookingDTO) {
        Booking existingBooking = bookingRepository.findById(id).orElseThrow(() -> new RuntimeException("Бронювання не знайдено"));
        bookingMapper.updateEntityFromDto(bookingDTO, existingBooking);

        Room room = roomRepository.findById(bookingDTO.getRoomId()).orElseThrow(() -> new RuntimeException("Кімнату не знайдено"));
        Guest guest = guestRepository.findById(bookingDTO.getGuestId()).orElseThrow(() -> new RuntimeException("Гостя не знайдено"));

        existingBooking.setRoom(room);
        existingBooking.setGuest(guest);

        return bookingMapper.toDto(bookingRepository.save(existingBooking));
    }

    public void delete(Long id) {
        bookingRepository.deleteById(id);
    }
}