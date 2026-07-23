package com.fx.api.model;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** DTO with validation (deck 05: DTOs, Validation, Logging). */
public record ConversionRequest(
        @Pattern(regexp = "[A-Z]{3}", message = "base must be a 3-letter code") String base,
        @Pattern(regexp = "[A-Z]{3}", message = "quote must be a 3-letter code") String quote,
        @Positive(message = "amount must be positive") double amount) {
}
