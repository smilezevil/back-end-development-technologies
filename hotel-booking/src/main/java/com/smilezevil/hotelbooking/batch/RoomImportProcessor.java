package com.smilezevil.hotelbooking.batch;

import com.smilezevil.hotelbooking.entity.Hotel;
import com.smilezevil.hotelbooking.entity.Room;
import com.smilezevil.hotelbooking.repository.HotelRepository;
import com.smilezevil.hotelbooking.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class RoomImportProcessor implements ItemProcessor<RoomCsvRow, Room> {

    static final List<String> ROOM_TYPES = List.of("STANDARD", "DELUXE", "LUXURY", "SUITE");
    static final int MAX_ROOM_NUMBER_LENGTH = 20;
    static final BigDecimal MAX_PRICE = new BigDecimal("100000");
    static final int MAX_CAPACITY = 10;

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final Map<String, Integer> firstLineByRoom = new HashMap<>();

    @Override
    public Room process(RoomCsvRow row) {
        long hotelId = hotelId(row);
        String roomNumber = roomNumber(row);
        String type = type(row);
        BigDecimal price = price(row);
        int capacity = capacity(row);

        Optional<Room> existing = roomRepository.findFirstByHotel_IdAndRoomNumberOrderByIdAsc(hotelId, roomNumber);
        if (existing.isPresent()) {
            rejectDuplicateInFile(row, hotelId, roomNumber);
            return updateIfChanged(existing.get(), type, price, capacity);
        }

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> invalid(row, "готелю з id " + hotelId + " не існує"));
        rejectDuplicateInFile(row, hotelId, roomNumber);

        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomNumber(roomNumber);
        room.setType(type);
        room.setPricePerNight(price);
        room.setCapacity(capacity);
        return room;
    }

    private Room updateIfChanged(Room room, String type, BigDecimal price, int capacity) {
        boolean unchanged = type.equals(room.getType())
                && room.getPricePerNight().compareTo(price) == 0
                && room.getCapacity() == capacity;
        if (unchanged) {
            return null;
        }
        room.setType(type);
        room.setPricePerNight(price);
        room.setCapacity(capacity);
        return room;
    }

    private void rejectDuplicateInFile(RoomCsvRow row, long hotelId, String roomNumber) {
        Integer firstLine = firstLineByRoom.putIfAbsent(hotelId + "/" + roomNumber, row.lineNumber());
        if (firstLine != null && firstLine != row.lineNumber()) {
            throw invalid(row, "кімната " + roomNumber + " готелю " + hotelId + " вже була в рядку " + firstLine);
        }
    }

    private static long hotelId(RoomCsvRow row) {
        String value = required(row, "hotelId", row.hotelId());
        Long id = parseLong(value);
        if (id == null || id <= 0) {
            throw invalid(row, "hotelId '" + value + "' має бути додатним цілим числом");
        }
        return id;
    }

    private static String roomNumber(RoomCsvRow row) {
        String value = required(row, "roomNumber", row.roomNumber());
        if (value.length() > MAX_ROOM_NUMBER_LENGTH) {
            throw invalid(row, "номер кімнати '" + value + "' довший за " + MAX_ROOM_NUMBER_LENGTH + " символів");
        }
        return value;
    }

    private static String type(RoomCsvRow row) {
        String value = required(row, "type", row.type()).toUpperCase(Locale.ROOT);
        if (!ROOM_TYPES.contains(value)) {
            throw invalid(row, "тип '" + row.type() + "' невідомий, допустимі: " + String.join(", ", ROOM_TYPES));
        }
        return value;
    }

    private static BigDecimal price(RoomCsvRow row) {
        String value = required(row, "pricePerNight", row.pricePerNight());
        BigDecimal price = parseDecimal(value);
        if (price == null) {
            throw invalid(row, "ціна '" + value + "' не є числом");
        }
        if (price.signum() <= 0) {
            throw invalid(row, "ціна має бути більшою за 0, а не " + value);
        }
        if (price.compareTo(MAX_PRICE) > 0) {
            throw invalid(row, "ціна " + value + " більша за допустимі " + MAX_PRICE);
        }
        if (price.stripTrailingZeros().scale() > 2) {
            throw invalid(row, "ціна " + value + " має більше двох знаків після коми");
        }
        return price.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static int capacity(RoomCsvRow row) {
        String value = required(row, "capacity", row.capacity());
        Integer capacity = parseInt(value);
        if (capacity == null || capacity < 1 || capacity > MAX_CAPACITY) {
            throw invalid(row, "місткість '" + value + "' має бути цілим числом від 1 до " + MAX_CAPACITY);
        }
        return capacity;
    }

    private static String required(RoomCsvRow row, String field, String value) {
        if (value == null || value.isBlank()) {
            throw invalid(row, "поле " + field + " порожнє");
        }
        return value.trim();
    }

    private static Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseInt(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal parseDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static InvalidRoomRowException invalid(RoomCsvRow row, String reason) {
        return new InvalidRoomRowException(row.lineNumber(), reason);
    }
}