package com.vitalair.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Static region + city reference data, bound from application.yml.
 * Mirrors the original REGIONS / CITIES dicts in main.py, but externalized
 * instead of hardcoded so new regions/cities can be added without a rebuild.
 */
@Data
@ConfigurationProperties(prefix = "vitalair")
public class RegionProperties {

    private Map<String, Region> regions;

    @Data
    public static class Region {
        private String name;
        private double latMin;
        private double latMax;
        private double lonMin;
        private double lonMax;
        private double centerLat;
        private double centerLon;
        private int zoom;
        private int defaultDensity;
        private List<CityDef> cities;
    }

    @Data
    public static class CityDef {
        private String name;
        private double lat;
        private double lon;
        private String type;
        private int baseAqi;
    }
}
