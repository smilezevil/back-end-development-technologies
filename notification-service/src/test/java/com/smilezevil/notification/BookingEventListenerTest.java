package com.smilezevil.notification;

import com.smilezevil.common.dto.NotificationReceipt;
import com.smilezevil.common.event.BookingEvent;
import com.smilezevil.common.event.BookingEventType;
import com.smilezevil.common.event.BookingTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookingEventListenerTest {

    private final NotificationService notificationService = new NotificationService();
    private final BookingEventListener listener = new BookingEventListener(notificationService);


    @Test
    void onBookingEvent_sameEventDeliveredTwice_resultStaysAsAfterFirstDelivery() {
        ConsumerRecord<String, BookingEvent> record =
                record(0, event("event-1", BookingEventType.CREATED, "guest@gmail.com"));

        listener.onBookingEvent(record);
        List<NotificationReceipt> afterFirstDelivery = notificationService.history();
        listener.onBookingEvent(record);
        List<NotificationReceipt> afterSecondDelivery = notificationService.history();

        assertThat(afterFirstDelivery).hasSize(1);
        assertThat(afterSecondDelivery).isEqualTo(afterFirstDelivery);
    }

    @Test
    void onBookingEvent_threeBusinessActions_createThreeVisibleNotifications() {
        listener.onBookingEvent(record(0, event("event-1", BookingEventType.CREATED, "guest@gmail.com")));
        listener.onBookingEvent(record(1, event("event-2", BookingEventType.UPDATED, "guest@gmail.com")));
        listener.onBookingEvent(record(2, event("event-3", BookingEventType.CANCELLED, "guest@gmail.com")));

        assertThat(notificationService.history())
                .extracting(NotificationReceipt::subject)
                .containsExactly(
                        "Booking #22 confirmed: 2026-12-01 - 2026-12-05",
                        "Booking #22 changed: 2026-12-01 - 2026-12-05",
                        "Booking #22 cancelled");
        assertThat(notificationService.history())
                .extracting(NotificationReceipt::status)
                .containsOnly("SENT");
    }

    @Test
    void onBookingEvent_rejectedRecipient_isRecordedAsRejected() {
        listener.onBookingEvent(record(0, event("event-9", BookingEventType.CREATED, "broken@hotel.invalid")));

        assertThat(notificationService.history())
                .singleElement()
                .extracting(NotificationReceipt::status)
                .isEqualTo("REJECTED");
    }


    private static ConsumerRecord<String, BookingEvent> record(long offset, BookingEvent event) {
        return new ConsumerRecord<>(BookingTopics.BOOKING_EVENTS, 0, offset, String.valueOf(event.bookingId()), event);
    }

    private static BookingEvent event(String eventId, BookingEventType type, String email) {
        return new BookingEvent(eventId, type, 22L, email,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), new BigDecimal("5400.00"));
    }
}