package com.smilezevil.hotelbooking.repository;

import com.smilezevil.hotelbooking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Booking b set b.status = :newStatus "
            + "where b.status = :currentStatus and b.checkInDate < :threshold")
    int updateStatusWhereCheckInBefore(@Param("currentStatus") String currentStatus,
                                       @Param("newStatus") String newStatus,
                                       @Param("threshold") LocalDate threshold);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Booking b set b.status = :newStatus "
            + "where b.status = :currentStatus and b.checkOutDate < :threshold")
    int updateStatusWhereCheckOutBefore(@Param("currentStatus") String currentStatus,
                                        @Param("newStatus") String newStatus,
                                        @Param("threshold") LocalDate threshold);
}