package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.dto.HotelDTO;
import com.smilezevil.hotelbooking.entity.Hotel;
import com.smilezevil.hotelbooking.mapper.HotelMapper;
import com.smilezevil.hotelbooking.repository.HotelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class HotelServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private HotelMapper hotelMapper;

    @InjectMocks
    private HotelService hotelService;


    @Test
    void create_validHotel_savesAndReturnsDto() {
        HotelDTO inputDto = hotelDto(null, "Bukovina Palace");
        Hotel hotel = hotel(null, "Bukovina Palace");
        Hotel savedHotel = hotel(1L, "Bukovina Palace");

        when(hotelMapper.toEntity(inputDto)).thenReturn(hotel);
        when(hotelRepository.save(hotel)).thenReturn(savedHotel);
        when(hotelMapper.toDto(savedHotel)).thenReturn(hotelDto(1L, "Bukovina Palace"));

        HotelDTO result = hotelService.create(inputDto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Bukovina Palace");
        verify(hotelRepository).save(hotel);
    }

    @Test
    void create_databaseError_throwsExceptionAndReturnsNothing() {
        HotelDTO inputDto = hotelDto(null, "Bukovina Palace");
        Hotel hotel = hotel(null, "Bukovina Palace");

        when(hotelMapper.toEntity(inputDto)).thenReturn(hotel);
        when(hotelRepository.save(hotel)).thenThrow(new RuntimeException("DB is down"));

        assertThatThrownBy(() -> hotelService.create(inputDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB is down");
        verify(hotelMapper, never()).toDto(any());
    }


    @Test
    void findAll_hotelsExist_returnsAllHotels() {
        Hotel first = hotel(1L, "Bukovina Palace");
        Hotel second = hotel(2L, "Viden");

        when(hotelRepository.findAll()).thenReturn(List.of(first, second));
        when(hotelMapper.toDto(first)).thenReturn(hotelDto(1L, "Bukovina Palace"));
        when(hotelMapper.toDto(second)).thenReturn(hotelDto(2L, "Viden"));

        List<HotelDTO> result = hotelService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Bukovina Palace");
        assertThat(result.get(1).getName()).isEqualTo("Viden");
    }

    @Test
    void findAll_noHotels_returnsEmptyList() {
        when(hotelRepository.findAll()).thenReturn(List.of());

        List<HotelDTO> result = hotelService.findAll();

        assertThat(result).isEmpty();
        verifyNoInteractions(hotelMapper);
    }


    @Test
    void findById_existingId_returnsHotel() {
        Hotel hotel = hotel(1L, "Bukovina Palace");

        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        when(hotelMapper.toDto(hotel)).thenReturn(hotelDto(1L, "Bukovina Palace"));

        HotelDTO result = hotelService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Bukovina Palace");
    }

    @Test
    void findById_nonExistingId_throwsNotFoundException() {
        when(hotelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hotelService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Готель з ID 99 не знайдено");
    }


    @Test
    void update_existingId_updatesAndSavesHotel() {
        Hotel existing = hotel(1L, "Old name");
        HotelDTO changes = hotelDto(null, "Viden");

        when(hotelRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(hotelRepository.save(existing)).thenReturn(existing);
        when(hotelMapper.toDto(existing)).thenReturn(hotelDto(1L, "Viden"));

        HotelDTO result = hotelService.update(1L, changes);

        assertThat(result.getName()).isEqualTo("Viden");
        verify(hotelMapper).updateEntityFromDto(changes, existing);
        verify(hotelRepository).save(existing);
    }

    @Test
    void update_nonExistingId_throwsAndDoesNotSave() {
        HotelDTO changes = hotelDto(null, "Viden");
        when(hotelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hotelService.update(99L, changes))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Готель з ID 99 не знайдено");
        verify(hotelRepository, never()).save(any());
        verifyNoInteractions(hotelMapper);
    }


    @Test
    void delete_existingId_callsRepositoryDelete() {
        hotelService.delete(1L);

        verify(hotelRepository).deleteById(1L);
    }

    @Test
    void delete_databaseError_throwsException() {
        doThrow(new RuntimeException("DB is down")).when(hotelRepository).deleteById(1L);

        assertThatThrownBy(() -> hotelService.delete(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB is down");
    }


    private static Hotel hotel(Long id, String name) {
        Hotel hotel = new Hotel();
        hotel.setId(id);
        hotel.setName(name);
        hotel.setCity("Чернівці");
        return hotel;
    }

    private static HotelDTO hotelDto(Long id, String name) {
        HotelDTO dto = new HotelDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setCity("Чернівці");
        return dto;
    }
}