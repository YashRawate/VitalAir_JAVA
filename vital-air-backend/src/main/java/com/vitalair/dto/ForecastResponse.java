package com.vitalair.dto;

import java.time.Instant;
import java.util.List;

public record ForecastResponse(
        double lat,
        double lon,
        String region,
        int currentAqi,
        int peakAqi,
        Instant peakTime,
        int avgAqi,
        List<ForecastPointDto> points,
        Instant generatedAt
) {
    public record ForecastPointDto(Instant time, int hourOfDay, int aqi, String category, String color) {
    }
}
