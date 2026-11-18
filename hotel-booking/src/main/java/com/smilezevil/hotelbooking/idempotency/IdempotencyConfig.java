package com.smilezevil.hotelbooking.idempotency;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class IdempotencyConfig {

    @Bean
    public IdempotencyStore idempotencyStore() {
        return new IdempotencyStore();
    }

    @Bean
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilter(IdempotencyStore idempotencyStore) {
        FilterRegistrationBean<IdempotencyFilter> registration =
                new FilterRegistrationBean<>(new IdempotencyFilter(idempotencyStore));
        registration.addUrlPatterns("/api/bookings");
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
        return registration;
    }
}