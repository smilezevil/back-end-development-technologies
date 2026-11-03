package com.smilezevil.hotelbooking.messaging;

import com.smilezevil.common.event.BookingEvent;
import com.smilezevil.common.event.BookingEventType;
import com.smilezevil.common.event.BookingTopics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingEventKafkaPublisherTest {

    @Mock
    private KafkaTemplate<String, BookingEvent> kafkaTemplate;

    @InjectMocks
    private BookingEventKafkaPublisher publisher;


    @Test
    void onBookingEvent_createdEvent_sendsToBookingTopicWithBookingIdAsKey() {
        BookingEvent event = new BookingEvent("event-1", BookingEventType.CREATED, 42L, "guest@gmail.com",
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), new BigDecimal("5400.00"));
        when(kafkaTemplate.send(anyString(), anyString(), any(BookingEvent.class)))
                .thenReturn(new CompletableFuture<>());

        publisher.onBookingEvent(event);

        verify(kafkaTemplate).send(BookingTopics.BOOKING_EVENTS, "42", event);
    }
}