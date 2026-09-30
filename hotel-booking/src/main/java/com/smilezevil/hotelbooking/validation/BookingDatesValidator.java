package com.smilezevil.hotelbooking.validation;

import com.smilezevil.hotelbooking.annotation.ValidBookingDates;
import com.smilezevil.hotelbooking.dto.BookingDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BookingDatesValidator implements ConstraintValidator<ValidBookingDates, BookingDTO> {

    @Override
    public boolean isValid(BookingDTO booking, ConstraintValidatorContext context) {
        if (booking == null || booking.getCheckInDate() == null || booking.getCheckOutDate() == null) {
            return true;
        }

        if (booking.getCheckOutDate().isAfter(booking.getCheckInDate())) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                        "Дата виїзду (" + booking.getCheckOutDate()
                                + ") має бути пізніше за дату заїзду (" + booking.getCheckInDate() + ")")
                .addPropertyNode("checkOutDate")
                .addConstraintViolation();
        return false;
    }
}