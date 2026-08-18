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
            return defaultZones(region, resolvedKey);
        }

        List<ZoneResponse> result = new ArrayList<>();
        Instant now = Instant.now();

        for (CityDataService.CityAqi city : cityData) {
            int level = levelForAqi(city.aqi());
            ZoneLevelDef def = ZONE_LEVELS.get(Math.min(level - 1, ZONE_LEVELS.size() - 1));
            double radius = round1(4.0 + (level * 0.8));

            String zoneName = city.name() + " · " + def.name();

            aqiZoneRepository.save(AqiZone.builder()
                    .region(resolvedKey).level(def.level()).name(zoneName).aqiRange(def.aqiRange())
                    .color(def.color()).radiusKm(radius)
                    .centerLat(city.lat()).centerLon(city.lon())
                    .cityCount(1).generatedAt(now)
                    .build());

            result.add(new ZoneResponse(def.level(), zoneName, def.aqiRange(), def.color(),
                    city.lat(), city.lon(), radius, 1));
        }
        return result;
    }

    private List<ZoneResponse> defaultZones(RegionProperties.Region region, String resolvedKey) {
        List<ZoneResponse> result = new ArrayList<>();
        double[][] coords = "delhi".equalsIgnoreCase(resolvedKey) ? new double[][]{
                {28.6139, 77.2090}, {28.6508, 77.3152}, {28.6297, 77.2427},
                {28.5665, 77.1767}, {28.5921, 77.0460}, {28.5355, 77.3910}
        } : new double[][]{
                {19.0760, 72.8777}, {19.2183, 72.9781}, {19.0330, 73.0297},
                {19.0896, 72.8656}, {18.5204, 73.8567}, {19.9975, 73.7898}
        };

        for (int i = 0; i < ZONE_LEVELS.size(); i++) {
            ZoneLevelDef def = ZONE_LEVELS.get(i);
            double lat = coords[i % coords.length][0];
            double lon = coords[i % coords.length][1];
            double radius = 4.0 + (def.level() * 0.8);
            result.add(new ZoneResponse(def.level(), def.name(), def.aqiRange(), def.color(),
                    lat, lon, radius, 1));
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
