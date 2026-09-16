package com.smilezevil.hotelbooking.mapper;

import com.smilezevil.hotelbooking.dto.GuestDTO;
import com.smilezevil.hotelbooking.entity.Guest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface GuestMapper {

    GuestDTO toDto(Guest guest);

    Guest toEntity(GuestDTO guestDTO);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(GuestDTO guestDTO, @MappingTarget Guest guest);
}