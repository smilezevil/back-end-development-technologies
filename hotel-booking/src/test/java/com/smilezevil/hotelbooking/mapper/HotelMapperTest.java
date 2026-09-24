package com.smilezevil.hotelbooking.mapper;

import com.smilezevil.hotelbooking.dto.HotelDTO;
import com.smilezevil.hotelbooking.entity.Hotel;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class HotelMapperTest {

    // Справжня реалізація, яку згенерував MapStruct (без Spring)
    private final HotelMapper hotelMapper = Mappers.getMapper(HotelMapper.class);

    // ===== toDto =====

    @Test
    void toDto_filledHotel_copiesAllFields() {
        // Arrange
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("Bukovina Palace");
        hotel.setAddress("вул. Головна, 141");
        hotel.setCity("Чернівці");
        hotel.setDescription("Готель у центрі міста.");

        // Act
        HotelDTO dto = hotelMapper.toDto(hotel);

        // Assert
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Bukovina Palace");
        assertThat(dto.getAddress()).isEqualTo("вул. Головна, 141");
        assertThat(dto.getCity()).isEqualTo("Чернівці");
        assertThat(dto.getDescription()).isEqualTo("Готель у центрі міста.");
    }

    @Test
    void toDto_nullHotel_returnsNull() {
        // Act
        HotelDTO dto = hotelMapper.toDto(null);

        // Assert
        assertThat(dto).isNull();
    }

    // ===== toEntity =====

    @Test
    void toEntity_filledDto_copiesAllFields() {
        // Arrange
        HotelDTO dto = new HotelDTO();
        dto.setName("Viden");
        dto.setAddress("Віденський проїзд, 5");
        dto.setCity("Чернівці");
        dto.setDescription("Преміум готель.");

        // Act
        Hotel hotel = hotelMapper.toEntity(dto);

        // Assert
        assertThat(hotel.getName()).isEqualTo("Viden");
        assertThat(hotel.getAddress()).isEqualTo("Віденський проїзд, 5");
        assertThat(hotel.getCity()).isEqualTo("Чернівці");
        assertThat(hotel.getDescription()).isEqualTo("Преміум готель.");
    }

    @Test
    void toEntity_nullDto_returnsNull() {
        // Act
        Hotel hotel = hotelMapper.toEntity(null);

        // Assert
        assertThat(hotel).isNull();
    }

    // ===== updateEntityFromDto =====

    @Test
    void updateEntityFromDto_newValues_updatesFieldsButKeepsId() {
        // Arrange
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("Old name");
        hotel.setCity("Київ");

        HotelDTO changes = new HotelDTO();
        changes.setId(999L);              // спробуємо підмінити id
        changes.setName("Viden");
        changes.setCity("Чернівці");

        // Act
        hotelMapper.updateEntityFromDto(changes, hotel);

        // Assert
        assertThat(hotel.getId()).isEqualTo(1L);   // id НЕ змінився
        assertThat(hotel.getName()).isEqualTo("Viden");
        assertThat(hotel.getCity()).isEqualTo("Чернівці");
    }

    @Test
    void updateEntityFromDto_nullDto_leavesHotelUnchanged() {
        // Arrange
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("Bukovina Palace");

        // Act
        hotelMapper.updateEntityFromDto(null, hotel);

        // Assert
        assertThat(hotel.getId()).isEqualTo(1L);
        assertThat(hotel.getName()).isEqualTo("Bukovina Palace");
    }
}