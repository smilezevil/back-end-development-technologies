package com.smilezevil.hotelbooking.controller;

import com.smilezevil.hotelbooking.dto.HotelDTO;
import com.smilezevil.hotelbooking.service.HotelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    @PreAuthorize("hasRole('ADMIN') or hasRole('admin')")
    @PostMapping
    public ResponseEntity<HotelDTO> createHotel(@Valid @RequestBody HotelDTO hotelDTO) {
        return new ResponseEntity<>(hotelService.create(hotelDTO), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<HotelDTO>> getAllHotels() {
        return ResponseEntity.ok(hotelService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HotelDTO> getHotelById(@PathVariable Long id) {
        return ResponseEntity.ok(hotelService.findById(id));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('admin')")
    @PutMapping("/{id}")
    public ResponseEntity<HotelDTO> updateHotel(@PathVariable Long id, @Valid @RequestBody HotelDTO hotelDTO) {
        return ResponseEntity.ok(hotelService.update(id, hotelDTO));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteHotel(@PathVariable Long id) {
        hotelService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Готель успішно видалено"));
    }
}