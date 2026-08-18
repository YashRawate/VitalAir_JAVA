package com.vitalair.service;

public record WeatherReading(
        Double temperature,
        Double humidity,
        Double pressure,
        Double windSpeed,
        Double windDirection,
        String condition,
        String source
) {
}
