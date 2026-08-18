package com.vitalair.dto;

public record LocationSearchResult(
        String name,
        double lat,
        double lon,
        String region,
        String type
) {
}
