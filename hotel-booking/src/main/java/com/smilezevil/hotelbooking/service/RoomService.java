package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.dto.RoomDTO;
import com.smilezevil.hotelbooking.entity.Hotel;
import com.smilezevil.hotelbooking.entity.Room;
import com.smilezevil.hotelbooking.mapper.RoomMapper;
import com.smilezevil.hotelbooking.repository.HotelRepository;
import com.smilezevil.hotelbooking.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomMapper roomMapper;

    public RoomDTO create(RoomDTO roomDTO) {
        Room room = roomMapper.toEntity(roomDTO);
        Hotel hotel = hotelRepository.findById(roomDTO.getHotelId())
                .orElseThrow(() -> new RuntimeException("Готель не знайдено"));
        room.setHotel(hotel);
        return roomMapper.toDto(roomRepository.save(room));
    }

    public List<RoomDTO> findAll() {
        return roomRepository.findAll().stream().map(roomMapper::toDto).collect(Collectors.toList());
    }

    public RoomDTO findById(Long id) {
        return roomRepository.findById(id).map(roomMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Кімнату не знайдено"));
    }

    public RoomDTO update(Long id, RoomDTO roomDTO) {
        Room existingRoom = roomRepository.findById(id).orElseThrow(() -> new RuntimeException("Кімнату не знайдено"));
        roomMapper.updateEntityFromDto(roomDTO, existingRoom);

        Hotel hotel = hotelRepository.findById(roomDTO.getHotelId())
                .orElseThrow(() -> new RuntimeException("Готель не знайдено"));
        existingRoom.setHotel(hotel);

        return roomMapper.toDto(roomRepository.save(existingRoom));
    }

    public void delete(Long id) {
        roomRepository.deleteById(id);
    }
}