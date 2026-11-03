package com.smilezevil.notification;

import com.smilezevil.common.event.BookingEvent;
import com.smilezevil.common.event.BookingTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BookingEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookingEventListener.class);

    private final NotificationService notificationService;

    public BookingEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = BookingTopics.BOOKING_EVENTS, groupId = BookingTopics.NOTIFICATION_GROUP)
    public void onBookingEvent(ConsumerRecord<String, BookingEvent> record) {
        BookingEvent event = record.value();
        log.info("RECEIVED {} for booking {} from partition {} offset {} (eventId={})",
                event.type(), event.bookingId(), record.partition(), record.offset(), event.eventId());
        notificationService.notifyAbout(event);
    }
}