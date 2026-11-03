package com.smilezevil.hotelbooking.messaging;

import com.smilezevil.common.event.BookingEvent;
import com.smilezevil.common.event.BookingEventType;
import com.smilezevil.common.event.BookingTopics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
class BookingEventAfterCommitTest {

    @Configuration
    @EnableTransactionManagement
    @Import(BookingEventKafkaPublisher.class)
    static class Config {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new NoOpTransactionManager();
        }
    }

    static class NoOpTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }

    @MockBean
    private KafkaTemplate<String, BookingEvent> kafkaTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void stubKafka() {
        when(kafkaTemplate.send(anyString(), anyString(), any(BookingEvent.class)))
                .thenReturn(new CompletableFuture<>());
    }


    @Test
    void eventPublishedInsideTransaction_isSentOnlyAfterCommit() {
        BookingEvent event = event();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any(BookingEvent.class));
        });

        verify(kafkaTemplate).send(BookingTopics.BOOKING_EVENTS, "42", event);
    }

    @Test
    void eventPublishedInsideTransaction_isNotSentWhenTransactionRollsBack() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event());
            throw new IllegalStateException("Room is already booked for these dates");
        })).isInstanceOf(IllegalStateException.class);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(BookingEvent.class));
    }

    @Test
    void eventPublishedWithoutTransaction_isNotSent() {
        eventPublisher.publishEvent(event());

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(BookingEvent.class));
    }


    private static BookingEvent event() {
        return new BookingEvent("event-42", BookingEventType.CREATED, 42L, "guest@gmail.com",
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), new BigDecimal("5400.00"));
    }
}