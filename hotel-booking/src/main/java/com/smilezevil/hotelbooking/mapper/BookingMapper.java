package com.smilezevil.hotelbooking.mapper;

import com.smilezevil.hotelbooking.dto.BookingDTO;
import com.smilezevil.hotelbooking.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(source = "room.id", target = "roomId")
    @Mapping(source = "guest.id", target = "guestId")
    BookingDTO toDto(Booking booking);

    @Mapping(target = "room", ignore = true)
    @Mapping(target = "guest", ignore = true)
    Booking toEntity(BookingDTO bookingDTO);

    @Mapping(target = "room", ignore = true)
    @Mapping(target = "guest", ignore = true)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(BookingDTO bookingDTO, @MappingTarget Booking booking);
}