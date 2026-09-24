package com.smilezevil.hotelbooking;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Інтеграційний тест: потребує PostgreSQL і Keycloak, не є unit-тестом")   // ← додати
@SpringBootTest
class HotelBookingApplicationTests {

    @Test
    void contextLoads() {
    }

}
