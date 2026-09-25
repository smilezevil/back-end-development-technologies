package com.smilezevil.hotelbooking.mapper;

import com.smilezevil.hotelbooking.dto.GuestDTO;
import com.smilezevil.hotelbooking.entity.Guest;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class GuestMapperTest {

    private final GuestMapper guestMapper = Mappers.getMapper(GuestMapper.class);


    @Test
    void toDto_filledGuest_copiesAllFields() {
        Guest guest = new Guest();
        guest.setId(1L);
        guest.setFirstName("Анастасія");
        guest.setLastName("Дем'янчук");
        guest.setEmail("nastya.d@gmail.com");
        guest.setPhone("+380501112233");

        GuestDTO dto = guestMapper.toDto(guest);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getFirstName()).isEqualTo("Анастасія");
        assertThat(dto.getLastName()).isEqualTo("Дем'янчук");
        assertThat(dto.getEmail()).isEqualTo("nastya.d@gmail.com");
        assertThat(dto.getPhone()).isEqualTo("+380501112233");
    }

    @Test
    void toDto_nullGuest_returnsNull() {
        GuestDTO dto = guestMapper.toDto(null);

        assertThat(dto).isNull();
    }


    @Test
    void toEntity_filledDto_copiesAllFields() {
        GuestDTO dto = new GuestDTO();
        dto.setFirstName("Катерина");
        dto.setLastName("Дорофтей");
        dto.setEmail("kate.d@gmail.com");
        dto.setPhone("+380509998877");

        Guest guest = guestMapper.toEntity(dto);

        assertThat(guest.getFirstName()).isEqualTo("Катерина");
        assertThat(guest.getLastName()).isEqualTo("Дорофтей");
        assertThat(guest.getEmail()).isEqualTo("kate.d@gmail.com");
        assertThat(guest.getPhone()).isEqualTo("+380509998877");
    }

    @Test
    void toEntity_nullDto_returnsNull() {
        Guest guest = guestMapper.toEntity(null);

        assertThat(guest).isNull();
    }


    @Test
    void updateEntityFromDto_newValues_updatesFieldsButKeepsId() {
        Guest guest = new Guest();
        guest.setId(1L);
        guest.setEmail("old@gmail.com");

        GuestDTO changes = new GuestDTO();
        changes.setId(777L);
        changes.setEmail("new@gmail.com");

        guestMapper.updateEntityFromDto(changes, guest);

        assertThat(guest.getId()).isEqualTo(1L);
        assertThat(guest.getEmail()).isEqualTo("new@gmail.com");
    }

    @Test
    void updateEntityFromDto_nullDto_leavesGuestUnchanged() {
        Guest guest = new Guest();
        guest.setId(1L);
        guest.setEmail("nastya.d@gmail.com");

        guestMapper.updateEntityFromDto(null, guest);

        assertThat(guest.getId()).isEqualTo(1L);
        assertThat(guest.getEmail()).isEqualTo("nastya.d@gmail.com");
    }
}