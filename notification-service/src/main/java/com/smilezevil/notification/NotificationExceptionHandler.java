package com.smilezevil.notification;

import com.smilezevil.common.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class NotificationExceptionHandler {

    @ExceptionHandler(RecipientRejectedException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiError rejected(RecipientRejectedException ex) {
        return new ApiError(HttpStatus.UNPROCESSABLE_ENTITY.value(), ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError badRequest(IllegalArgumentException ex) {
        return new ApiError(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    }
}