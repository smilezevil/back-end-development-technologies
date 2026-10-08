package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.annotation.RetryOnFailure;
import com.smilezevil.hotelbooking.annotation.NormalizeInput;
import com.smilezevil.hotelbooking.dto.HotelDTO;
import com.smilezevil.hotelbooking.entity.Hotel;
import com.smilezevil.hotelbooking.mapper.HotelMapper;
import com.smilezevil.hotelbooking.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final HotelMapper hotelMapper;

    @NormalizeInput
    public HotelDTO create(HotelDTO hotelDTO) {
        Hotel hotel = hotelMapper.toEntity(hotelDTO);
        Hotel savedHotel = hotelRepository.save(hotel);
        return hotelMapper.toDto(savedHotel);
    }

    @RetryOnFailure
    public List<HotelDTO> findAll() {
        return hotelRepository.findAll().stream()
                .map(hotelMapper::toDto)
                .collect(Collectors.toList());
    }

    @RetryOnFailure
    public HotelDTO findById(Long id) {
        return hotelRepository.findById(id)
                .map(hotelMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Готель з ID " + id + " не знайдено"));
    }

    @NormalizeInput
    public HotelDTO update(Long id, HotelDTO hotelDTO) {
        Hotel existingHotel = hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Готель з ID " + id + " не знайдено"));

        hotelMapper.updateEntityFromDto(hotelDTO, existingHotel);
        Hotel updatedHotel = hotelRepository.save(existingHotel);
        return hotelMapper.toDto(updatedHotel);
    }

    public void delete(Long id) {
        hotelRepository.deleteById(id);
    }
}