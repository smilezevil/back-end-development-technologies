package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.dto.GuestDTO;
import com.smilezevil.hotelbooking.entity.Guest;
import com.smilezevil.hotelbooking.mapper.GuestMapper;
import com.smilezevil.hotelbooking.repository.GuestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

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
class GuestServiceTest {

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private GuestMapper guestMapper;

    @InjectMocks
    private GuestService guestService;

    // ===== create =====

    @Test
    void create_validGuest_savesAndReturnsDto() {
        // Arrange
        GuestDTO inputDto = guestDto(null, "nastya.d@gmail.com");
        Guest guest = guest(null, "nastya.d@gmail.com");
        Guest savedGuest = guest(1L, "nastya.d@gmail.com");

        when(guestMapper.toEntity(inputDto)).thenReturn(guest);
        when(guestRepository.save(guest)).thenReturn(savedGuest);
        when(guestMapper.toDto(savedGuest)).thenReturn(guestDto(1L, "nastya.d@gmail.com"));

        // Act
        GuestDTO result = guestService.create(inputDto);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("nastya.d@gmail.com");
        verify(guestRepository).save(guest);
    }

    @Test
    void create_duplicateEmail_throwsExceptionAndReturnsNothing() {
        // Arrange
        GuestDTO inputDto = guestDto(null, "nastya.d@gmail.com");
        Guest guest = guest(null, "nastya.d@gmail.com");

        when(guestMapper.toEntity(inputDto)).thenReturn(guest);
        when(guestRepository.save(guest))
                .thenThrow(new DataIntegrityViolationException("duplicate email"));

        // Act + Assert
        assertThatThrownBy(() -> guestService.create(inputDto))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(guestMapper, never()).toDto(any());
    }

    // ===== findAll =====

    @Test
    void findAll_guestsExist_returnsAllGuests() {
        // Arrange
        Guest first = guest(1L, "nastya.d@gmail.com");
        Guest second = guest(2L, "kate.d@gmail.com");

        when(guestRepository.findAll()).thenReturn(List.of(first, second));
        when(guestMapper.toDto(first)).thenReturn(guestDto(1L, "nastya.d@gmail.com"));
        when(guestMapper.toDto(second)).thenReturn(guestDto(2L, "kate.d@gmail.com"));

        // Act
        List<GuestDTO> result = guestService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmail()).isEqualTo("nastya.d@gmail.com");
        assertThat(result.get(1).getEmail()).isEqualTo("kate.d@gmail.com");
    }

    @Test
    void findAll_noGuests_returnsEmptyList() {
        // Arrange
        when(guestRepository.findAll()).thenReturn(List.of());

        // Act
        List<GuestDTO> result = guestService.findAll();

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(guestMapper);
    }

    // ===== findById =====

    @Test
    void findById_existingId_returnsGuest() {
        // Arrange
        Guest guest = guest(1L, "nastya.d@gmail.com");

        when(guestRepository.findById(1L)).thenReturn(Optional.of(guest));
        when(guestMapper.toDto(guest)).thenReturn(guestDto(1L, "nastya.d@gmail.com"));

        // Act
        GuestDTO result = guestService.findById(1L);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("nastya.d@gmail.com");
    }

    @Test
    void findById_nonExistingId_throwsNotFoundException() {
        // Arrange
        when(guestRepository.findById(42L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> guestService.findById(42L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Гостя не знайдено");
    }

    // ===== update =====

    @Test
    void update_existingId_updatesAndSavesGuest() {
        // Arrange
        Guest existing = guest(1L, "old@gmail.com");
        GuestDTO changes = guestDto(null, "new@gmail.com");

        when(guestRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(guestRepository.save(existing)).thenReturn(existing);
        when(guestMapper.toDto(existing)).thenReturn(guestDto(1L, "new@gmail.com"));

        // Act
        GuestDTO result = guestService.update(1L, changes);

        // Assert
        assertThat(result.getEmail()).isEqualTo("new@gmail.com");
        verify(guestMapper).updateEntityFromDto(changes, existing);
        verify(guestRepository).save(existing);
    }

    @Test
    void update_nonExistingId_throwsAndDoesNotSave() {
        // Arrange
        GuestDTO changes = guestDto(null, "new@gmail.com");
        when(guestRepository.findById(42L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> guestService.update(42L, changes))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Гостя не знайдено");
        verify(guestRepository, never()).save(any());
        verifyNoInteractions(guestMapper);
    }

    // ===== delete =====

    @Test
    void delete_existingId_callsRepositoryDelete() {
        // Act
        guestService.delete(1L);

        // Assert
        verify(guestRepository).deleteById(1L);
    }

    @Test
    void delete_databaseError_throwsException() {
        // Arrange
        doThrow(new RuntimeException("DB is down")).when(guestRepository).deleteById(1L);

        // Act + Assert
        assertThatThrownBy(() -> guestService.delete(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB is down");
    }

    // ===== помічники для створення тестових даних =====

    private static Guest guest(Long id, String email) {
        Guest guest = new Guest();
        guest.setId(id);
        guest.setFirstName("Анастасія");
        guest.setLastName("Дем'янчук");
        guest.setEmail(email);
        return guest;
    }

    private static GuestDTO guestDto(Long id, String email) {
        GuestDTO dto = new GuestDTO();
        dto.setId(id);
        dto.setFirstName("Анастасія");
        dto.setLastName("Дем'янчук");
        dto.setEmail(email);
        return dto;
    }
}