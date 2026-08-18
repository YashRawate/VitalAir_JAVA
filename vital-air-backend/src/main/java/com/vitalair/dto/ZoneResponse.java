package com.vitalair.dto;

public record ZoneResponse(
        int level,
        String name,
        String aqiRange,
        String color,
        double centerLat,
        double centerLon,
        double radiusKm,
        int cityCount
) {
}
