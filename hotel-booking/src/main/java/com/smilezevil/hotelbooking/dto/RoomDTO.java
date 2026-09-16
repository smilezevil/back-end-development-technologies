package com.smilezevil.hotelbooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RoomDTO {
    private Long id;

    @NotNull
    private Long hotelId;

    @NotBlank
    private String roomNumber;

    @NotBlank
    private String type;

    @NotNull
    private BigDecimal pricePerNight;

    @NotNull
    private Integer capacity;
}