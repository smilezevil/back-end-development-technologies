package com.smilezevil.hotelbooking.controller;

import com.smilezevil.hotelbooking.dto.ImportStartedDTO;
import com.smilezevil.hotelbooking.dto.ImportStatusDTO;
import com.smilezevil.hotelbooking.service.RoomImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/imports/rooms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('admin')")
public class RoomImportController {

    private final RoomImportService roomImportService;

    @PostMapping
    public ResponseEntity<ImportStartedDTO> start(@RequestParam String file,
                                                  @RequestParam(defaultValue = "false") boolean rerun) {
        ImportStartedDTO started = roomImportService.start(file, rerun);
        return ResponseEntity.accepted()
                .location(URI.create("/api/imports/rooms/" + started.executionId()))
                .body(started);
    }

    @GetMapping("/{executionId}")
    public ImportStatusDTO status(@PathVariable Long executionId) {
        return roomImportService.status(executionId);
    }
}