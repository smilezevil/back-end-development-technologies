package com.smilezevil.hotelbooking.annotation;

import com.smilezevil.hotelbooking.validation.BookingDatesValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BookingDatesValidator.class)
@Documented
public @interface ValidBookingDates {

    String message() default "Дата виїзду має бути пізніше за дату заїзду";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}