package com.smilezevil.hotelbooking.mapper;

import com.smilezevil.hotelbooking.dto.GuestDTO;
import com.smilezevil.hotelbooking.entity.Guest;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class GuestMapperTest {

    private final GuestMapper guestMapper = Mappers.getMapper(GuestMapper.class);

    // ===== toDto =====

    @Test
    void toDto_filledGuest_copiesAllFields() {
        // Arrange
        Guest guest = new Guest();
        guest.setId(1L);
        guest.setFirstName("Анастасія");
        guest.setLastName("Дем'янчук");
        guest.setEmail("nastya.d@gmail.com");
        guest.setPhone("+380501112233");

        // Act
        GuestDTO dto = guestMapper.toDto(guest);

        // Assert
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getFirstName()).isEqualTo("Анастасія");
        assertThat(dto.getLastName()).isEqualTo("Дем'янчук");
        assertThat(dto.getEmail()).isEqualTo("nastya.d@gmail.com");
        assertThat(dto.getPhone()).isEqualTo("+380501112233");
    }

    @Test
    void toDto_nullGuest_returnsNull() {
        // Act
        GuestDTO dto = guestMapper.toDto(null);

        // Assert
        assertThat(dto).isNull();
    }

    // ===== toEntity =====

    @Test
    void toEntity_filledDto_copiesAllFields() {
        // Arrange
        GuestDTO dto = new GuestDTO();
        dto.setFirstName("Катерина");
        dto.setLastName("Дорофтей");
        dto.setEmail("kate.d@gmail.com");
        dto.setPhone("+380509998877");

        // Act
        Guest guest = guestMapper.toEntity(dto);

        // Assert
        assertThat(guest.getFirstName()).isEqualTo("Катерина");
        assertThat(guest.getLastName()).isEqualTo("Дорофтей");
        assertThat(guest.getEmail()).isEqualTo("kate.d@gmail.com");
        assertThat(guest.getPhone()).isEqualTo("+380509998877");
    }

    @Test
    void toEntity_nullDto_returnsNull() {
        // Act
        Guest guest = guestMapper.toEntity(null);

        // Assert
        assertThat(guest).isNull();
    }

    // ===== updateEntityFromDto =====

    @Test
    void updateEntityFromDto_newValues_updatesFieldsButKeepsId() {
        // Arrange
        Guest guest = new Guest();
        guest.setId(1L);
        guest.setEmail("old@gmail.com");

        GuestDTO changes = new GuestDTO();
        changes.setId(777L);              // спробуємо підмінити id
        changes.setEmail("new@gmail.com");

        // Act
        guestMapper.updateEntityFromDto(changes, guest);

        // Assert
        assertThat(guest.getId()).isEqualTo(1L);   // id НЕ змінився
        assertThat(guest.getEmail()).isEqualTo("new@gmail.com");
    }

    @Test
    void updateEntityFromDto_nullDto_leavesGuestUnchanged() {
        // Arrange
        Guest guest = new Guest();
        guest.setId(1L);
        guest.setEmail("nastya.d@gmail.com");

        // Act
        guestMapper.updateEntityFromDto(null, guest);

        // Assert
        assertThat(guest.getId()).isEqualTo(1L);
        assertThat(guest.getEmail()).isEqualTo("nastya.d@gmail.com");
    }
}