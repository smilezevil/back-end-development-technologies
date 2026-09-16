package com.smilezevil.hotelbooking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BookingDTO {
    private Long id;

    @NotNull
    private Long roomId;

    @NotNull
    private Long guestId;

    @NotNull
    private LocalDate checkInDate;

    @NotNull
    private LocalDate checkOutDate;

    private String status;
    private BigDecimal totalPrice;
}