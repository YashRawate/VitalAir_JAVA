package com.vitalair.service;

/**
 * Normalized air-quality reading, regardless of which upstream API produced it.
 * Mirrors the dict shape returned by fetch_openweather_aqi / fetch_openaq_aqi /
 * fetch_openmeteo_aqi in the original main.py.
 */
public record AqiReading(
        Double pm25,
        Double pm10,
        Double no2,
        Double so2,
        Double co,
        Double o3,
        int aqi,
        long timestampEpochSeconds,
        String source
) {
}
