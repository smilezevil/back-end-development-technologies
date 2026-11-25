package com.smilezevil.hotelbooking.controller;

import com.smilezevil.hotelbooking.dto.RevisionDTO;
import com.smilezevil.hotelbooking.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('admin')")
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping("/hotels/{id}")
    public List<RevisionDTO> hotelHistory(@PathVariable Long id) {
        return historyService.hotelHistory(id);
    }

    @GetMapping("/rooms/{id}")
    public List<RevisionDTO> roomHistory(@PathVariable Long id) {
        return historyService.roomHistory(id);
    }

    @GetMapping("/bookings/{id}")
    public List<RevisionDTO> bookingHistory(@PathVariable Long id) {
        return historyService.bookingHistory(id);
    }
}