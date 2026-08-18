package com.vitalair.dto;

import jakarta.validation.constraints.NotNull;

public record PredictionRequest(
        @NotNull Double lat,
        @NotNull Double lon
) {
}
