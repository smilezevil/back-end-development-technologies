package com.smilezevil.hotelbooking.controller;

import com.smilezevil.hotelbooking.annotation.PostCreated;
import com.smilezevil.hotelbooking.dto.GuestDTO;
import com.smilezevil.hotelbooking.service.GuestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/guests")
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;

    @PreAuthorize("hasRole('ADMIN') or hasRole('admin')")
    @PostCreated
    public GuestDTO createGuest(@Valid @RequestBody GuestDTO dto) {
        return guestService.create(dto);
    }

    @GetMapping
    public ResponseEntity<List<GuestDTO>> getAllGuests() {
        return ResponseEntity.ok(guestService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuestDTO> getGuestById(@PathVariable Long id) {
        return ResponseEntity.ok(guestService.findById(id));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('admin')")
    @PutMapping("/{id}")
    public ResponseEntity<GuestDTO> updateGuest(@PathVariable Long id, @Valid @RequestBody GuestDTO dto) {
        return ResponseEntity.ok(guestService.update(id, dto));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteGuest(@PathVariable Long id) {
        guestService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Гостя успішно видалено"));
    }
}