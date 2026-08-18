package com.vitalair.dto;

import jakarta.validation.constraints.NotNull;

public record RouteRequest(
        @NotNull Double startLat,
        @NotNull Double startLon,
        @NotNull Double endLat,
        @NotNull Double endLon
) {
}
