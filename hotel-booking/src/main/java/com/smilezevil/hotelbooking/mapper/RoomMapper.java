package com.smilezevil.hotelbooking.mapper;

import com.smilezevil.hotelbooking.dto.RoomDTO;
import com.smilezevil.hotelbooking.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    @Mapping(source = "hotel.id", target = "hotelId")
    RoomDTO toDto(Room room);

    @Mapping(target = "hotel", ignore = true)
    Room toEntity(RoomDTO roomDTO);

    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(RoomDTO roomDTO, @MappingTarget Room room);
}