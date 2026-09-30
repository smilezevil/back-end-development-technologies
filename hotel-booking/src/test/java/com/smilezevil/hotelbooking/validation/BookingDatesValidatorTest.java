package com.smilezevil.hotelbooking.validation;

import com.smilezevil.hotelbooking.annotation.ValidBookingDates;
import com.smilezevil.hotelbooking.dto.BookingDTO;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BookingDatesValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }


    @Test
    void validBookingDates_onBookingDto_linkedToBookingDatesValidator() {
        ValidBookingDates annotationOnDto = BookingDTO.class.getAnnotation(ValidBookingDates.class);
        Constraint constraint = ValidBookingDates.class.getAnnotation(Constraint.class);

        assertThat(annotationOnDto).isNotNull();
        assertThat(constraint.validatedBy()).containsExactly(BookingDatesValidator.class);
    }


    @ParameterizedTest(name = "заїзд {0}, виїзд {1} — валідно")
    @CsvSource({
            "2026-10-10, 2026-10-11",
            "2026-10-10, 2026-10-15",
            "2026-12-30, 2027-01-02"
    })
    void validate_checkOutAfterCheckIn_noViolations(LocalDate checkIn, LocalDate checkOut) {
        BookingDTO booking = booking(checkIn, checkOut);

        Set<ConstraintViolation<BookingDTO>> violations = validator.validate(booking);

        assertThat(violations).isEmpty();
    }


    @Test
    void validate_checkOutBeforeCheckIn_violationWithClearMessage() {
        BookingDTO booking = booking(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 10));

        Set<ConstraintViolation<BookingDTO>> violations = validator.validate(booking);

        assertThat(violations).hasSize(1);
        ConstraintViolation<BookingDTO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("checkOutDate");
        assertThat(violation.getMessage())
                .isEqualTo("Дата виїзду (2026-10-10) має бути пізніше за дату заїзду (2026-10-15)");
    }

    @Test
    void validate_checkOutSameDayAsCheckIn_violationWithClearMessage() {
        BookingDTO booking = booking(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 10));

        Set<ConstraintViolation<BookingDTO>> violations = validator.validate(booking);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Дата виїзду (2026-10-10) має бути пізніше за дату заїзду (2026-10-10)");
    }


    @Test
    void validate_missingCheckOutDate_onlyNotNullViolation() {
        BookingDTO booking = booking(LocalDate.of(2026, 10, 10), null);

        Set<ConstraintViolation<BookingDTO>> violations = validator.validate(booking);

        assertThat(violations).hasSize(1);
        ConstraintViolation<BookingDTO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("checkOutDate");
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType())
                .isEqualTo(NotNull.class);
    }


    private static BookingDTO booking(LocalDate checkIn, LocalDate checkOut) {
        BookingDTO dto = new BookingDTO();
        dto.setRoomId(1L);
        dto.setGuestId(1L);
        dto.setCheckInDate(checkIn);
        dto.setCheckOutDate(checkOut);
        return dto;
    }
}