package com.smilezevil.hotelbooking.batch;

import com.smilezevil.hotelbooking.entity.Hotel;
import com.smilezevil.hotelbooking.entity.Room;
import com.smilezevil.hotelbooking.repository.HotelRepository;
import com.smilezevil.hotelbooking.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomImportProcessorTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelRepository hotelRepository;

    private RoomImportProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new RoomImportProcessor(roomRepository, hotelRepository);
    }

    @Test
    void process_newRoom_createsRoomForHotel() {
        Hotel hotel = hotel(1L);
        when(roomRepository.findFirstByHotel_IdAndRoomNumberOrderByIdAsc(1L, "301")).thenReturn(Optional.empty());
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));

        Room room = processor.process(row(2, "1", "301", "standard", "1200", "2"));

        assertThat(room.getId()).isNull();
        assertThat(room.getHotel()).isSameAs(hotel);
        assertThat(room.getRoomNumber()).isEqualTo("301");
        assertThat(room.getType()).isEqualTo("STANDARD");
        assertThat(room.getPricePerNight()).isEqualByComparingTo("1200.00");
        assertThat(room.getCapacity()).isEqualTo(2);
    }

    @Test
    void process_existingRoomWithChanges_updatesSameRoom() {
        Room existing = room(10L, "STANDARD", "1200.00", 2);
        when(roomRepository.findFirstByHotel_IdAndRoomNumberOrderByIdAsc(1L, "301")).thenReturn(Optional.of(existing));

        Room room = processor.process(row(3, "1", "301", "SUITE", "3500", "4"));

        assertThat(room).isSameAs(existing);
        assertThat(room.getType()).isEqualTo("SUITE");
        assertThat(room.getPricePerNight()).isEqualByComparingTo("3500");
        assertThat(room.getCapacity()).isEqualTo(4);
    }

    @Test
    void process_existingRoomWithoutChanges_returnsNullToFilterIt() {
        Room existing = room(10L, "STANDARD", "1200.00", 2);
        when(roomRepository.findFirstByHotel_IdAndRoomNumberOrderByIdAsc(1L, "301")).thenReturn(Optional.of(existing));

        assertThat(processor.process(row(4, "1", "301", "STANDARD", "1200", "2"))).isNull();
    }

    @ParameterizedTest(name = "{5}")
    @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
            "abc|301|STANDARD|1200|2|hotelId 'abc' має бути додатним цілим числом",
            "-5|301|STANDARD|1200|2|hotelId '-5' має бути додатним цілим числом",
            "1||STANDARD|1200|2|поле roomNumber порожнє",
            "1|ROOM-NUMBER-TOO-LONG-123|STANDARD|1200|2|довший за 20 символів",
            "1|301|PENTHOUSE|1200|2|тип 'PENTHOUSE' невідомий",
            "1|301|STANDARD|abc|2|ціна 'abc' не є числом",
            "1|301|STANDARD|0|2|ціна має бути більшою за 0",
            "1|301|STANDARD|-100|2|ціна має бути більшою за 0",
            "1|301|STANDARD|200000|2|ціна 200000 більша за допустимі 100000",
            "1|301|STANDARD|12.345|2|має більше двох знаків після коми",
            "1|301|STANDARD|1200|0|місткість '0' має бути цілим числом від 1 до 10",
            "1|301|STANDARD|1200|11|місткість '11' має бути цілим числом від 1 до 10",
            "1|301|STANDARD|1200|two|місткість 'two' має бути цілим числом від 1 до 10"
    })
    void process_invalidRow_throwsWithLineNumberAndReason(String hotelId, String roomNumber, String type,
                                                          String price, String capacity, String reason) {
        assertThatThrownBy(() -> processor.process(row(7, hotelId, roomNumber, type, price, capacity)))
                .isInstanceOf(InvalidRoomRowException.class)
                .hasMessageStartingWith("Рядок 7: ")
                .hasMessageContaining(reason);
    }

    @Test
    void process_unknownHotel_throwsWithLineNumber() {
        when(roomRepository.findFirstByHotel_IdAndRoomNumberOrderByIdAsc(99L, "301")).thenReturn(Optional.empty());
        when(hotelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processor.process(row(5, "99", "301", "STANDARD", "1200", "2")))
                .isInstanceOf(InvalidRoomRowException.class)
                .hasMessage("Рядок 5: готелю з id 99 не існує");
    }

    @Test
    void process_sameRoomTwiceInFile_secondLineIsRejected() {
        when(roomRepository.findFirstByHotel_IdAndRoomNumberOrderByIdAsc(1L, "301")).thenReturn(Optional.empty());
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel(1L)));

        processor.process(row(2, "1", "301", "STANDARD", "1200", "2"));

        assertThatThrownBy(() -> processor.process(row(6, "1", "301", "SUITE", "3000", "3")))
                .isInstanceOf(InvalidRoomRowException.class)
                .hasMessage("Рядок 6: кімната 301 готелю 1 вже була в рядку 2");
    }

    @Test
    void process_sameLineAgainAfterChunkRollback_isNotTreatedAsDuplicate() {
        when(roomRepository.findFirstByHotel_IdAndRoomNumberOrderByIdAsc(1L, "301")).thenReturn(Optional.empty());
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel(1L)));

        processor.process(row(2, "1", "301", "STANDARD", "1200", "2"));
        Room again = processor.process(row(2, "1", "301", "STANDARD", "1200", "2"));

        assertThat(again.getRoomNumber()).isEqualTo("301");
    }

    private static RoomCsvRow row(int line, String hotelId, String roomNumber, String type,
                                  String price, String capacity) {
        return new RoomCsvRow(line, hotelId, roomNumber, type, price, capacity);
    }

    private static Hotel hotel(Long id) {
        Hotel hotel = new Hotel();
        hotel.setId(id);
        return hotel;
    }

    private static Room room(Long id, String type, String price, int capacity) {
        Room room = new Room();
        room.setId(id);
        room.setHotel(hotel(1L));
        room.setRoomNumber("301");
        room.setType(type);
        room.setPricePerNight(new BigDecimal(price));
        room.setCapacity(capacity);
        return room;
    }
}