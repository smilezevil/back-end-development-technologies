package com.smilezevil.hotelbooking.mapper;

import com.smilezevil.hotelbooking.dto.RoomDTO;
import com.smilezevil.hotelbooking.entity.Hotel;
import com.smilezevil.hotelbooking.entity.Room;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RoomMapperTest {

    private final RoomMapper roomMapper = Mappers.getMapper(RoomMapper.class);


    @Test
    void toDto_roomWithHotel_mapsHotelIdAndFields() {
        Hotel hotel = new Hotel();
        hotel.setId(7L);

        Room room = new Room();
        room.setId(10L);
        room.setHotel(hotel);
        room.setRoomNumber("101");
        room.setType("STANDARD");
        room.setPricePerNight(new BigDecimal("1200.00"));
        room.setCapacity(2);

        RoomDTO dto = roomMapper.toDto(room);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getHotelId()).isEqualTo(7L);
        assertThat(dto.getRoomNumber()).isEqualTo("101");
        assertThat(dto.getType()).isEqualTo("STANDARD");
        assertThat(dto.getPricePerNight()).isEqualTo(new BigDecimal("1200.00"));
        assertThat(dto.getCapacity()).isEqualTo(2);
    }

    @Test
    void toDto_roomWithoutHotel_returnsNullHotelId() {
        Room room = new Room();
        room.setId(10L);
        room.setRoomNumber("101");

        RoomDTO dto = roomMapper.toDto(room);

        assertThat(dto.getHotelId()).isNull();
        assertThat(dto.getRoomNumber()).isEqualTo("101");
    }

    @Test
    void toDto_nullRoom_returnsNull() {
        RoomDTO dto = roomMapper.toDto(null);

        assertThat(dto).isNull();
    }


    @Test
    void toEntity_filledDto_copiesFieldsButIgnoresHotel() {
        RoomDTO dto = new RoomDTO();
        dto.setHotelId(7L);
        dto.setRoomNumber("VIP-1");
        dto.setType("LUXURY");
        dto.setPricePerNight(new BigDecimal("3500.00"));
        dto.setCapacity(4);

        Room room = roomMapper.toEntity(dto);

        assertThat(room.getHotel()).isNull();
        assertThat(room.getRoomNumber()).isEqualTo("VIP-1");
        assertThat(room.getType()).isEqualTo("LUXURY");
        assertThat(room.getPricePerNight()).isEqualTo(new BigDecimal("3500.00"));
        assertThat(room.getCapacity()).isEqualTo(4);
    }

    @Test
    void toEntity_nullDto_returnsNull() {
        Room room = roomMapper.toEntity(null);

        assertThat(room).isNull();
    }


    @Test
    void updateEntityFromDto_newValues_updatesFieldsButKeepsIdAndHotel() {
        Hotel hotel = new Hotel();
        hotel.setId(1L);

        Room room = new Room();
        room.setId(10L);
        room.setHotel(hotel);
        room.setType("STANDARD");

        RoomDTO changes = new RoomDTO();
        changes.setId(555L);
        changes.setHotelId(2L);
        changes.setType("LUXURY");

        roomMapper.updateEntityFromDto(changes, room);

        assertThat(room.getId()).isEqualTo(10L);
        assertThat(room.getHotel()).isSameAs(hotel);
        assertThat(room.getType()).isEqualTo("LUXURY");
    }

    @Test
    void updateEntityFromDto_nullDto_leavesRoomUnchanged() {
        Room room = new Room();
        room.setId(10L);
        room.setType("STANDARD");

        roomMapper.updateEntityFromDto(null, room);

        assertThat(room.getId()).isEqualTo(10L);
        assertThat(room.getType()).isEqualTo("STANDARD");
    }
}