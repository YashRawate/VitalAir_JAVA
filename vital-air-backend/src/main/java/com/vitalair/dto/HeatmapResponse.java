package com.vitalair.dto;

import java.time.Instant;
import java.util.List;

public record HeatmapResponse(
        String region,
        List<HeatPoint> points,
        Instant generatedAt
) {
    public record HeatPoint(double lat, double lon, int aqi, double intensity) {
    }
}
