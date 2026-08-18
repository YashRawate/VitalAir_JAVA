package com.vitalair.dto;

public record PollutionSourceResponse(
        String type,
        double lat,
        double lon,
        String label,
        double intensity
) {
}
