package com.vitalair.dto;

public record HotspotResponse(
        String name,
        double lat,
        double lon,
        int aqi,
        String category,
        String type
) {
}
