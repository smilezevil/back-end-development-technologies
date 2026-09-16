package com.smilezevil.hotelbooking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HotelDTO {
    private Long id;

    @NotBlank(message = "Назва готелю не може бути порожньою")
    private String name;

    private String address;
    private String city;
    private String description;
}