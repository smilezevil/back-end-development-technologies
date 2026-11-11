package com.smilezevil.hotelbooking.rates;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "rates")
public record RatesProperties(@Valid @NotNull Rest rest, @Valid @NotNull Grpc grpc) {

    public record Rest(@NotBlank String url, @NotNull Duration connectTimeout, @NotNull Duration readTimeout) {
    }

    public record Grpc(@NotBlank String target, @NotNull Duration deadline) {
    }
}