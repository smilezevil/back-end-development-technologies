package com.smilezevil.hotelbooking.batch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "import.rooms")
public record RoomImportProperties(@NotBlank String directory,
                                   @Positive int chunkSize,
                                   @PositiveOrZero int skipLimit) {
}