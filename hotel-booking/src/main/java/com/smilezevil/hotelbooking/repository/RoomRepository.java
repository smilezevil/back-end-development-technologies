package com.smilezevil.hotelbooking.repository;

import com.smilezevil.hotelbooking.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findFirstByHotel_IdAndRoomNumberOrderByIdAsc(Long hotelId, String roomNumber);
}