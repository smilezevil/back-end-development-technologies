package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.dto.GuestDTO;
import com.smilezevil.hotelbooking.entity.Guest;
import com.smilezevil.hotelbooking.mapper.GuestMapper;
import com.smilezevil.hotelbooking.repository.GuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestRepository guestRepository;
    private final GuestMapper guestMapper;

    public GuestDTO create(GuestDTO guestDTO) {
        Guest guest = guestMapper.toEntity(guestDTO);
        return guestMapper.toDto(guestRepository.save(guest));
    }

    public List<GuestDTO> findAll() {
        return guestRepository.findAll().stream().map(guestMapper::toDto).collect(Collectors.toList());
    }

    public GuestDTO findById(Long id) {
        return guestRepository.findById(id).map(guestMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Гостя не знайдено"));
    }

    public GuestDTO update(Long id, GuestDTO guestDTO) {
        Guest existingGuest = guestRepository.findById(id).orElseThrow(() -> new RuntimeException("Гостя не знайдено"));
        guestMapper.updateEntityFromDto(guestDTO, existingGuest);
        return guestMapper.toDto(guestRepository.save(existingGuest));
    }

    public void delete(Long id) {
        guestRepository.deleteById(id);
    }
}