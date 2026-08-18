package com.vitalair.service;

import com.vitalair.config.RegionProperties;
import com.vitalair.dto.ZoneResponse;
import com.vitalair.entity.AqiZone;
import com.vitalair.repository.AqiZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of get_zones / generate_default_zones in main.py: six concentric
 * severity rings sized by how many reference cities currently sit in each
 * AQI band, radiating out from the region's center point.
 */
@Service
public class ZoneService {

    private record ZoneLevelDef(int level, String name, String aqiRange, String color, int threshold) {
    }

    private static final List<ZoneLevelDef> ZONE_LEVELS = List.of(
            new ZoneLevelDef(1, "Zone 1 - Good", "0-50", "#00e400", 50),
            new ZoneLevelDef(2, "Zone 2 - Moderate", "51-100", "#ffff00", 100),
            new ZoneLevelDef(3, "Zone 3 - Unhealthy for Sensitive", "101-150", "#ff7e00", 150),
            new ZoneLevelDef(4, "Zone 4 - Unhealthy", "151-200", "#ff0000", 200),
            new ZoneLevelDef(5, "Zone 5 - Very Unhealthy", "201-300", "#8f3f97", 300),
            new ZoneLevelDef(6, "Zone 6 - Hazardous", "300+", "#7e0023", 500)
    );

    private final RegionService regionService;
    private final CityDataService cityDataService;
    private final AqiZoneRepository aqiZoneRepository;

    public ZoneService(RegionService regionService, CityDataService cityDataService,
                        AqiZoneRepository aqiZoneRepository) {
        this.regionService = regionService;
        this.cityDataService = cityDataService;
        this.aqiZoneRepository = aqiZoneRepository;
    }

    @Transactional
    public List<ZoneResponse> zones(String regionKey) {
        RegionProperties.Region region = regionService.region(regionKey)
                .orElseGet(() -> regionService.region("delhi").orElseThrow());
        String resolvedKey = regionService.region(regionKey).isPresent() ? regionKey : "delhi";

        List<CityDataService.CityAqi> cityData = cityDataService.fetchAllCitiesData(resolvedKey);
        if (cityData.isEmpty()) {
            return defaultZones(region);
        }

        Map<Integer, Integer> zoneCounts = new HashMap<>();
        for (CityDataService.CityAqi city : cityData) {
            int level = levelForAqi(city.aqi());
            zoneCounts.merge(level, 1, Integer::sum);
        }

        double maxRadius = "delhi".equals(resolvedKey) ? 30 : 100;
        List<ZoneResponse> result = new ArrayList<>();
        Instant now = Instant.now();

        for (ZoneLevelDef def : ZONE_LEVELS) {
            int cityCount = zoneCounts.getOrDefault(def.level(), 0);
            double radius;
            if (cityCount > 0) {
                double baseRadius = 5 + (def.level() * 2);
                double countFactor = Math.min(cityCount / (double) cityData.size() * 2, 1.5);
                radius = Math.min(baseRadius * countFactor, maxRadius);
            } else {
                radius = 3 + def.level();
            }

            aqiZoneRepository.save(AqiZone.builder()
                    .region(resolvedKey).level(def.level()).name(def.name()).aqiRange(def.aqiRange())
                    .color(def.color()).radiusKm(radius)
                    .centerLat(region.getCenterLat()).centerLon(region.getCenterLon())
                    .cityCount(cityCount).generatedAt(now)
                    .build());

            result.add(new ZoneResponse(def.level(), def.name(), def.aqiRange(), def.color(),
                    region.getCenterLat(), region.getCenterLon(), round1(radius), cityCount));
        }
        return result;
    }

    private List<ZoneResponse> defaultZones(RegionProperties.Region region) {
        List<ZoneResponse> result = new ArrayList<>();
        for (ZoneLevelDef def : ZONE_LEVELS) {
            double radius = 5 + (def.level() * 2);
            result.add(new ZoneResponse(def.level(), def.name(), def.aqiRange(), def.color(),
                    region.getCenterLat(), region.getCenterLon(), radius, 0));
        }
        return result;
    }

    /** Generates the 36-point polygon ring for a zone (used by the frontend map layer). */
    public List<double[]> polygonFor(double centerLat, double centerLon, double radiusKm) {
        List<double[]> points = new ArrayList<>();
        int numPoints = 36;
        for (int i = 0; i < numPoints; i++) {
            double angle = (360.0 / numPoints) * i;
            double rad = Math.toRadians(angle);
            double latOffset = radiusKm / 111.0 * Math.cos(rad);
            double lonOffset = radiusKm / (111.0 * Math.cos(Math.toRadians(centerLat))) * Math.sin(rad);
            points.add(new double[]{
                    round6(centerLat + latOffset), round6(centerLon + lonOffset)
            });
        }
        points.add(points.get(0)); // close polygon
        return points;
    }

    private static int levelForAqi(int aqi) {
        for (ZoneLevelDef def : ZONE_LEVELS) {
            if (aqi <= def.threshold()) {
                return def.level();
            }
        }
        return 6;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double round6(double v) {
        return Math.round(v * 1_000_000.0) / 1_000_000.0;
    }
}
