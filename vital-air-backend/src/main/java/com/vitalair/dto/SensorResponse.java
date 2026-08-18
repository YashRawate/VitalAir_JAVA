package com.vitalair.dto;

import java.time.Instant;

public record SensorResponse(
        String locationName,
        double lat,
        double lon,
        int aqi,
        String category,
        String color,
        Double pm25,
        String source,
        Instant recordedAt
) {
}
