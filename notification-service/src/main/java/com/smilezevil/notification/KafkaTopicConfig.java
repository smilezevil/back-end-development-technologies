package com.smilezevil.notification;

import com.smilezevil.common.event.BookingTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic bookingEventsTopic() {
        return TopicBuilder.name(BookingTopics.BOOKING_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}