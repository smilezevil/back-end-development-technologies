package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.dto.RoomDTO;
import com.smilezevil.hotelbooking.entity.Hotel;
import com.smilezevil.hotelbooking.entity.Room;
import com.smilezevil.hotelbooking.mapper.RoomMapper;
import com.smilezevil.hotelbooking.repository.HotelRepository;
import com.smilezevil.hotelbooking.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomService roomService;

    @Captor
    private ArgumentCaptor<Room> roomCaptor;


    @Test
    void create_existingHotel_linksHotelAndSavesRoom() {
        Hotel hotel = hotel(1L);
        RoomDTO inputDto = roomDto(null, 1L);
        Room room = room(null, null);
        Room savedRoom = room(10L, hotel);

        when(roomMapper.toEntity(inputDto)).thenReturn(room);
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        when(roomRepository.save(any(Room.class))).thenReturn(savedRoom);
        when(roomMapper.toDto(savedRoom)).thenReturn(roomDto(10L, 1L));

        RoomDTO result = roomService.create(inputDto);

        verify(roomRepository).save(roomCaptor.capture());
        Room roomPassedToSave = roomCaptor.getValue();
        assertThat(roomPassedToSave.getHotel()).isSameAs(hotel);
        assertThat(roomPassedToSave.getRoomNumber()).isEqualTo("101");

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getHotelId()).isEqualTo(1L);
    }

    @Test
    void create_nonExistingHotel_throwsAndDoesNotSave() {
        RoomDTO inputDto = roomDto(null, 99L);

        when(roomMapper.toEntity(inputDto)).thenReturn(room(null, null));
        when(hotelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.create(inputDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Готель не знайдено");
        verify(roomRepository, never()).save(any());
    }


    @Test
    void findAll_roomsExist_returnsAllRooms() {
        Room first = room(10L, hotel(1L));
        Room second = room(11L, hotel(1L));

        when(roomRepository.findAll()).thenReturn(List.of(first, second));
        when(roomMapper.toDto(first)).thenReturn(roomDto(10L, 1L));
        when(roomMapper.toDto(second)).thenReturn(roomDto(11L, 1L));

        List<RoomDTO> result = roomService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        assertThat(result.get(1).getId()).isEqualTo(11L);
    }

    @Test
    void findAll_noRooms_returnsEmptyList() {
        when(roomRepository.findAll()).thenReturn(List.of());

        List<RoomDTO> result = roomService.findAll();

        assertThat(result).isEmpty();
        verifyNoInteractions(roomMapper);
    }


    @Test
    void findById_existingId_returnsRoom() {
        Room room = room(10L, hotel(1L));

        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomMapper.toDto(room)).thenReturn(roomDto(10L, 1L));

        RoomDTO result = roomService.findById(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getRoomNumber()).isEqualTo("101");
    }

    @Test
    void findById_nonExistingId_throwsNotFoundException() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Кімнату не знайдено");
    }


    @Test
    void update_existingRoomAndHotel_movesRoomToNewHotelAndSaves() {
        Hotel oldHotel = hotel(1L);
        Hotel newHotel = hotel(2L);
        Room existing = room(10L, oldHotel);
        RoomDTO changes = roomDto(null, 2L);

        when(roomRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(hotelRepository.findById(2L)).thenReturn(Optional.of(newHotel));
        when(roomRepository.save(any(Room.class))).thenReturn(existing);
        when(roomMapper.toDto(existing)).thenReturn(roomDto(10L, 2L));

        RoomDTO result = roomService.update(10L, changes);

        verify(roomMapper).updateEntityFromDto(changes, existing);
        verify(roomRepository).save(roomCaptor.capture());
        assertThat(roomCaptor.getValue().getHotel()).isSameAs(newHotel);
        assertThat(result.getHotelId()).isEqualTo(2L);
    }

    @Test
    void update_nonExistingRoom_throwsAndDoesNotSearchHotel() {
        RoomDTO changes = roomDto(null, 1L);
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.update(99L, changes))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Кімнату не знайдено");
        verifyNoInteractions(hotelRepository);
        verify(roomRepository, never()).save(any());
    }

    @Test
    void update_nonExistingHotel_throwsAndDoesNotSave() {
        Room existing = room(10L, hotel(1L));
        RoomDTO changes = roomDto(null, 99L);

        when(roomRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(hotelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.update(10L, changes))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Готель не знайдено");
        verify(roomRepository, never()).save(any());
    }


    @Test
    void delete_existingId_callsRepositoryDelete() {
        roomService.delete(10L);

        verify(roomRepository).deleteById(10L);
    }

    @Test
    void delete_databaseError_throwsException() {
        doThrow(new RuntimeException("DB is down")).when(roomRepository).deleteById(10L);

        assertThatThrownBy(() -> roomService.delete(10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB is down");
    }


    private static Hotel hotel(Long id) {
        Hotel hotel = new Hotel();
        hotel.setId(id);
        hotel.setName("Hotel " + id);
        return hotel;
    }

    private static Room room(Long id, Hotel hotel) {
        Room room = new Room();
        room.setId(id);
        room.setHotel(hotel);
        room.setRoomNumber("101");
        room.setType("STANDARD");
        room.setPricePerNight(new BigDecimal("1200.00"));
        room.setCapacity(2);
        return room;
    }

    private static RoomDTO roomDto(Long id, Long hotelId) {
        RoomDTO dto = new RoomDTO();
        dto.setId(id);
        dto.setHotelId(hotelId);
        dto.setRoomNumber("101");
        dto.setType("STANDARD");
        dto.setPricePerNight(new BigDecimal("1200.00"));
        dto.setCapacity(2);
        return dto;
    }
}