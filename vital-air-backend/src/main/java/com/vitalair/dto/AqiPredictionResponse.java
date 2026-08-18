package com.vitalair.dto;

public record AqiPredictionResponse(
        double lat,
        double lon,
        int aqi,
        String category,
        String color,
        String source,
        int confidence,
        Double pm25,
        Double pm10,
        Double no2,
        Double co,
        Double o3,
        Double so2,
        Double temperature,
        Double humidity,
        Double windSpeed
) {
}
