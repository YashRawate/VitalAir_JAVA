package com.vitalair.service;

import com.vitalair.config.RegionProperties;
import com.vitalair.dto.HeatmapResponse;
import com.vitalair.entity.SensorReading;
import com.vitalair.repository.SensorReadingRepository;
import com.vitalair.service.interpolation.InterpolationService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Port of /heatmap. The original built a fine 20m-resolution grid per
 * request by fanning out live API calls across many points; that's too
 * expensive to do synchronously per request here, so this version
 * interpolates from the latest persisted SensorReading rows (populated by
 * the scheduled collector/processor jobs) using the pluggable
 * InterpolationService - giving the same shape of output with a much
 * cheaper request path.
 */
@Service
public class HeatmapService {

    private static final int GRID_SIZE = 20;

    private final SensorReadingRepository sensorReadingRepository;
    private final RegionService regionService;
    private final InterpolationService interpolationService;
    private final CityDataService cityDataService;

    public HeatmapService(SensorReadingRepository sensorReadingRepository, RegionService regionService,
                           InterpolationService interpolationService, CityDataService cityDataService) {
        this.sensorReadingRepository = sensorReadingRepository;
        this.regionService = regionService;
        this.interpolationService = interpolationService;
        this.cityDataService = cityDataService;
    }

    public HeatmapResponse heatmap(String regionKey) {
        RegionProperties.Region region = regionService.region(regionKey)
                .orElseGet(() -> regionService.region("delhi").orElseThrow());
        String resolvedKey = regionService.region(regionKey).isPresent() ? regionKey : "delhi";

        List<SensorReading> readings = sensorReadingRepository.findByRegionAndRecordedAtAfter(
                resolvedKey, Instant.now().minus(6, ChronoUnit.HOURS));

        // No persisted sensor history yet (e.g. fresh deployment before the first
        // scheduled collection run) - seed the grid from a live city-data fetch instead.
        if (readings.isEmpty()) {
            readings = cityDataService.fetchAllCitiesData(resolvedKey).stream()
                    .map(c -> SensorReading.builder()
                            .locationName(c.name()).lat(c.lat()).lon(c.lon()).region(resolvedKey)
                            .aqi(c.aqi()).recordedAt(Instant.now()).build())
                    .toList();
        }

        List<HeatmapResponse.HeatPoint> points = new ArrayList<>();
        double latStep = (region.getLatMax() - region.getLatMin()) / GRID_SIZE;
        double lonStep = (region.getLonMax() - region.getLonMin()) / GRID_SIZE;

        for (int i = 0; i <= GRID_SIZE; i++) {
            for (int j = 0; j <= GRID_SIZE; j++) {
                double lat = region.getLatMin() + i * latStep;
                double lon = region.getLonMin() + j * lonStep;
                double interpolated = interpolationService.interpolate(lat, lon, readings);
                if (Double.isNaN(interpolated)) {
                    continue;
                }
                int aqi = (int) Math.round(interpolated);
                double intensity = Math.min(1.0, aqi / 300.0);
                points.add(new HeatmapResponse.HeatPoint(round6(lat), round6(lon), aqi, round2(intensity)));
            }
        }

        return new HeatmapResponse(resolvedKey, points, Instant.now());
    }

    private static double round6(double v) {
        return Math.round(v * 1_000_000.0) / 1_000_000.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
