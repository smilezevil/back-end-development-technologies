package com.smilezevil.hotelbooking.mapper;

import com.smilezevil.hotelbooking.dto.HotelDTO;
import com.smilezevil.hotelbooking.entity.Hotel;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class HotelMapperTest {

    private final HotelMapper hotelMapper = Mappers.getMapper(HotelMapper.class);


    @Test
    void toDto_filledHotel_copiesAllFields() {
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("Bukovina Palace");
        hotel.setAddress("вул. Головна, 141");
        hotel.setCity("Чернівці");
        hotel.setDescription("Готель у центрі міста.");

        HotelDTO dto = hotelMapper.toDto(hotel);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Bukovina Palace");
        assertThat(dto.getAddress()).isEqualTo("вул. Головна, 141");
        assertThat(dto.getCity()).isEqualTo("Чернівці");
        assertThat(dto.getDescription()).isEqualTo("Готель у центрі міста.");
    }

    @Test
    void toDto_nullHotel_returnsNull() {
        HotelDTO dto = hotelMapper.toDto(null);

        assertThat(dto).isNull();
    }


    @Test
    void toEntity_filledDto_copiesAllFields() {
        HotelDTO dto = new HotelDTO();
        dto.setName("Viden");
        dto.setAddress("Віденський проїзд, 5");
        dto.setCity("Чернівці");
        dto.setDescription("Преміум готель.");

        Hotel hotel = hotelMapper.toEntity(dto);

        assertThat(hotel.getName()).isEqualTo("Viden");
        assertThat(hotel.getAddress()).isEqualTo("Віденський проїзд, 5");
        assertThat(hotel.getCity()).isEqualTo("Чернівці");
        assertThat(hotel.getDescription()).isEqualTo("Преміум готель.");
    }

    @Test
    void toEntity_nullDto_returnsNull() {
        Hotel hotel = hotelMapper.toEntity(null);

        assertThat(hotel).isNull();
    }


    @Test
    void updateEntityFromDto_newValues_updatesFieldsButKeepsId() {
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("Old name");
        hotel.setCity("Київ");

        HotelDTO changes = new HotelDTO();
        changes.setId(999L);
        changes.setName("Viden");
        changes.setCity("Чернівці");

        hotelMapper.updateEntityFromDto(changes, hotel);

        assertThat(hotel.getId()).isEqualTo(1L);
        assertThat(hotel.getName()).isEqualTo("Viden");
        assertThat(hotel.getCity()).isEqualTo("Чернівці");
    }

    @Test
    void updateEntityFromDto_nullDto_leavesHotelUnchanged() {
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("Bukovina Palace");

        hotelMapper.updateEntityFromDto(null, hotel);

        assertThat(hotel.getId()).isEqualTo(1L);
        assertThat(hotel.getName()).isEqualTo("Bukovina Palace");
    }
}