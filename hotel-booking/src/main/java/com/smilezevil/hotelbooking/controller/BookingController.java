package com.smilezevil.hotelbooking.controller;

import com.smilezevil.hotelbooking.dto.BookingDTO;
import com.smilezevil.hotelbooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PreAuthorize("hasRole('ADMIN') or hasRole('admin')")
    @PostMapping
    public ResponseEntity<BookingDTO> createBooking(@Valid @RequestBody BookingDTO dto) {
        return new ResponseEntity<>(bookingService.create(dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BookingDTO>> getAllBookings() {
        return ResponseEntity.ok(bookingService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingDTO> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.findById(id));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('admin')")
    @PutMapping("/{id}")
    public ResponseEntity<BookingDTO> updateBooking(@PathVariable Long id, @Valid @RequestBody BookingDTO dto) {
        return ResponseEntity.ok(bookingService.update(id, dto));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteBooking(@PathVariable Long id) {
        bookingService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Бронювання успішно видалено"));
    }
}