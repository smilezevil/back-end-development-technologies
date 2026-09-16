package com.smilezevil.hotelbooking.mapper;

import com.smilezevil.hotelbooking.dto.HotelDTO;
import com.smilezevil.hotelbooking.entity.Hotel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface HotelMapper {

    HotelDTO toDto(Hotel hotel);

    Hotel toEntity(HotelDTO hotelDTO);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(HotelDTO hotelDTO, @MappingTarget Hotel hotel);
}