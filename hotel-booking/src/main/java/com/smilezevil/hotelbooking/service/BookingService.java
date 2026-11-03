package com.smilezevil.hotelbooking.service;

import com.smilezevil.common.event.BookingEvent;
import com.smilezevil.common.event.BookingEventType;
import com.smilezevil.hotelbooking.dto.BookingDTO;
import com.smilezevil.hotelbooking.entity.Booking;
import com.smilezevil.hotelbooking.entity.Guest;
import com.smilezevil.hotelbooking.entity.Room;
import com.smilezevil.hotelbooking.mapper.BookingMapper;
import com.smilezevil.hotelbooking.repository.BookingRepository;
import com.smilezevil.hotelbooking.repository.GuestRepository;
import com.smilezevil.hotelbooking.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final BookingMapper bookingMapper;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BookingDTO create(BookingDTO bookingDTO) {
        Booking booking = bookingMapper.toEntity(bookingDTO);
        Room room = roomRepository.findById(bookingDTO.getRoomId()).orElseThrow(() -> new RuntimeException("Кімнату не знайдено"));
        Guest guest = guestRepository.findById(bookingDTO.getGuestId()).orElseThrow(() -> new RuntimeException("Гостя не знайдено"));

        booking.setRoom(room);
        booking.setGuest(guest);
        Booking savedBooking = bookingRepository.save(booking);
        BookingDTO saved = bookingMapper.toDto(savedBooking);

        notificationService.sendBookingConfirmation(saved.getId(), guest.getEmail());
        eventPublisher.publishEvent(toEvent(BookingEventType.CREATED, savedBooking));
        return saved;
    }

    public List<BookingDTO> findAll() {
        return bookingRepository.findAll().stream().map(bookingMapper::toDto).collect(Collectors.toList());
    }

    public BookingDTO findById(Long id) {
        return bookingRepository.findById(id).map(bookingMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Бронювання не знайдено"));
    }

    @Transactional
    public BookingDTO update(Long id, BookingDTO bookingDTO) {
        Booking existingBooking = bookingRepository.findById(id).orElseThrow(() -> new RuntimeException("Бронювання не знайдено"));
        bookingMapper.updateEntityFromDto(bookingDTO, existingBooking);

        Room room = roomRepository.findById(bookingDTO.getRoomId()).orElseThrow(() -> new RuntimeException("Кімнату не знайдено"));
        Guest guest = guestRepository.findById(bookingDTO.getGuestId()).orElseThrow(() -> new RuntimeException("Гостя не знайдено"));

        existingBooking.setRoom(room);
        existingBooking.setGuest(guest);

        Booking updatedBooking = bookingRepository.save(existingBooking);
        eventPublisher.publishEvent(toEvent(BookingEventType.UPDATED, updatedBooking));
        return bookingMapper.toDto(updatedBooking);
    }

    @Transactional
    public void delete(Long id) {
        Booking booking = bookingRepository.findById(id).orElseThrow(() -> new RuntimeException("Бронювання не знайдено"));
        BookingEvent cancelled = toEvent(BookingEventType.CANCELLED, booking);
        bookingRepository.delete(booking);
        eventPublisher.publishEvent(cancelled);
    }

    private BookingEvent toEvent(BookingEventType type, Booking booking) {
        return new BookingEvent(
                UUID.randomUUID().toString(),
                type,
                booking.getId(),
                booking.getGuest().getEmail(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getTotalPrice());
    }
}