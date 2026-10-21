package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.entity.Booking;
import com.smilezevil.hotelbooking.entity.Guest;
import com.smilezevil.hotelbooking.entity.Hotel;
import com.smilezevil.hotelbooking.entity.Room;
import com.smilezevil.hotelbooking.repository.BookingRepository;
import com.smilezevil.hotelbooking.repository.GuestRepository;
import com.smilezevil.hotelbooking.repository.HotelRepository;
import com.smilezevil.hotelbooking.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static com.smilezevil.hotelbooking.service.BookingMaintenanceService.CANCELLED;
import static com.smilezevil.hotelbooking.service.BookingMaintenanceService.COMPLETED;
import static com.smilezevil.hotelbooking.service.BookingMaintenanceService.CONFIRMED;
import static com.smilezevil.hotelbooking.service.BookingMaintenanceService.PENDING;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BookingMaintenanceService.class)
class BookingMaintenanceServiceIT {

    @Autowired
    private BookingMaintenanceService bookingMaintenanceService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private GuestRepository guestRepository;

    private final LocalDate today = LocalDate.now(BookingMaintenanceService.HOTEL_ZONE);
    private Room room;
    private Guest guest;

    @BeforeEach
    void setUp() {
        Hotel hotel = new Hotel();
        hotel.setName("IT Hotel");
        hotel.setCity("Чернівці");
        hotelRepository.save(hotel);

        room = new Room();
        room.setHotel(hotel);
        room.setRoomNumber("IT-1");
        room.setType("STANDARD");
        room.setPricePerNight(new BigDecimal("1000.00"));
        room.setCapacity(2);
        roomRepository.save(room);

        guest = new Guest();
        guest.setFirstName("Інтеграційний");
        guest.setLastName("Тест");
        guest.setEmail("it-" + System.nanoTime() + "@test.com");
        guestRepository.save(guest);
    }


    @Test
    void cancelStalePendingBookings_runTwice_secondRunChangesNothing() {
        Long stalePending = saveBooking(PENDING, today.minusDays(3), today.minusDays(1));
        Long upcomingPending = saveBooking(PENDING, today.plusDays(5), today.plusDays(7));
        Long confirmed = saveBooking(CONFIRMED, today.minusDays(3), today.plusDays(1));

        int firstRun = bookingMaintenanceService.cancelStalePendingBookings(0);
        List<String> afterFirstRun = statuses(stalePending, upcomingPending, confirmed);
        int secondRun = bookingMaintenanceService.cancelStalePendingBookings(0);
        List<String> afterSecondRun = statuses(stalePending, upcomingPending, confirmed);

        assertThat(firstRun).isGreaterThanOrEqualTo(1);
        assertThat(afterFirstRun).containsExactly(CANCELLED, PENDING, CONFIRMED);
        assertThat(secondRun).isZero();
        assertThat(afterSecondRun).isEqualTo(afterFirstRun);
    }

    @Test
    void completeFinishedStays_runTwice_secondRunChangesNothing() {
        Long finishedStay = saveBooking(CONFIRMED, today.minusDays(5), today.minusDays(2));
        Long ongoingStay = saveBooking(CONFIRMED, today.minusDays(1), today.plusDays(2));

        int firstRun = bookingMaintenanceService.completeFinishedStays(0);
        List<String> afterFirstRun = statuses(finishedStay, ongoingStay);
        int secondRun = bookingMaintenanceService.completeFinishedStays(0);
        List<String> afterSecondRun = statuses(finishedStay, ongoingStay);

        assertThat(firstRun).isGreaterThanOrEqualTo(1);
        assertThat(afterFirstRun).containsExactly(COMPLETED, CONFIRMED);
        assertThat(secondRun).isZero();
        assertThat(afterSecondRun).isEqualTo(afterFirstRun);
    }


    private Long saveBooking(String status, LocalDate checkIn, LocalDate checkOut) {
        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setGuest(guest);
        booking.setStatus(status);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setTotalPrice(new BigDecimal("1000.00"));
        return bookingRepository.save(booking).getId();
    }

    private List<String> statuses(Long... ids) {
        return Arrays.stream(ids)
                .map(id -> bookingRepository.findById(id).orElseThrow().getStatus())
                .toList();
    }
}