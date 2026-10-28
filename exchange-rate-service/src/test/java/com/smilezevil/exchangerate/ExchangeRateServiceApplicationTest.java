package com.smilezevil.exchangerate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ExchangeRateServiceApplicationTest {

    @Autowired
    private Environment environment;


    @Test
    void context_startsWithoutOtherServices_usesOwnNameAndPort() {
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("exchange-rate-service");
        assertThat(environment.getProperty("server.port")).isEqualTo("8081");
    }
}