package com.smilezevil.hotelbooking.messaging;

import com.smilezevil.common.event.BookingEvent;
import com.smilezevil.common.event.BookingTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventKafkaPublisher {

    private final KafkaTemplate<String, BookingEvent> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingEvent(BookingEvent event) {
        String key = String.valueOf(event.bookingId());
        log.info("PUBLISHING {} for booking {} to {} (key={}, eventId={})",
                event.type(), event.bookingId(), BookingTopics.BOOKING_EVENTS, key, event.eventId());
        kafkaTemplate.send(BookingTopics.BOOKING_EVENTS, key, event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Could not publish {} for booking {}: {}", event.type(), event.bookingId(), error.getMessage());
                    } else {
                        log.info("PUBLISHED {} for booking {} to partition {} at offset {}",
                                event.type(), event.bookingId(),
                                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }
}