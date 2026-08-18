package com.vitalair.service;

import com.vitalair.config.CacheConfig;
import com.vitalair.dto.ForecastResponse;
import com.vitalair.entity.Forecast;
import com.vitalair.entity.ForecastPoint;
import com.vitalair.exception.UpstreamDataException;
import com.vitalair.repository.ForecastRepository;
import com.vitalair.util.AqiCalculator;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Port of the /forecast endpoint. This is honestly a pattern-based estimate
 * (hour-of-day multipliers + a deterministic hash-derived jitter), not a
 * trained model - same as the original prototype. Kept faithful rather than
 * dressed up as ML, per the scoping decision for this migration.
 */
@Service
public class ForecastService {

    private static final Map<Integer, Double> TIME_PATTERNS = Map.ofEntries(
            Map.entry(0, 0.85), Map.entry(1, 0.82), Map.entry(2, 0.78), Map.entry(3, 0.75),
            Map.entry(4, 0.73), Map.entry(5, 0.75), Map.entry(6, 0.82), Map.entry(7, 0.95),
            Map.entry(8, 1.10), Map.entry(9, 1.15), Map.entry(10, 1.12), Map.entry(11, 1.08),
            Map.entry(12, 1.05), Map.entry(13, 1.02), Map.entry(14, 1.00), Map.entry(15, 0.98),
            Map.entry(16, 1.02), Map.entry(17, 1.10), Map.entry(18, 1.18), Map.entry(19, 1.15),
            Map.entry(20, 1.08), Map.entry(21, 1.00), Map.entry(22, 0.92), Map.entry(23, 0.88)
    );

    private static final Map<String, Double> REGION_FACTORS = Map.of("delhi", 1.1, "maharashtra", 0.9);

    private final AqiFetchService aqiFetchService;
    private final RegionService regionService;
    private final ForecastRepository forecastRepository;

    public ForecastService(AqiFetchService aqiFetchService, RegionService regionService,
                            ForecastRepository forecastRepository) {
        this.aqiFetchService = aqiFetchService;
        this.regionService = regionService;
        this.forecastRepository = forecastRepository;
    }

    @Cacheable(value = CacheConfig.FORECAST_CACHE,
            key = "'forecast_' + T(java.lang.Math).round(#lat) + '_' + T(java.lang.Math).round(#lon) + '_' + #hours")
    @Transactional
    public ForecastResponse forecast(double lat, double lon, int hoursRequested) {
        int hours = Math.min(hoursRequested, 48);
        String region = regionService.regionFromCoords(lat, lon);
        if (region == null) {
            region = "delhi";
        }

        AqiReading aqiData = aqiFetchService.fetchAnyAqi(lat, lon);
        if (aqiData == null) {
            throw new UpstreamDataException("No air quality data available for forecast");
        }
        int currentAqi = aqiData.aqi() > 0 ? aqiData.aqi() : 100;

        double regionFactor = REGION_FACTORS.getOrDefault(region, 1.0);
        ZonedDateTime baseTime = ZonedDateTime.now(ZoneId.systemDefault());

        List<ForecastResponse.ForecastPointDto> points = new ArrayList<>();
        List<ForecastPoint> entityPoints = new ArrayList<>();

        for (int i = 0; i < Math.min(hours, 48); i += 3) {
            ZonedDateTime forecastTime = baseTime.plusHours(i);
            int hour = forecastTime.getHour();
            double timeFactor = TIME_PATTERNS.getOrDefault(hour, 1.0);
            double seed = seedFraction(lat, lon, i);
            double randomFactor = 0.96 + 0.08 * seed;

            double forecastAqiRaw = currentAqi * timeFactor * regionFactor * randomFactor;
            forecastAqiRaw = Math.max(10, Math.min(500, forecastAqiRaw));
            int forecastAqi = (int) Math.round(forecastAqiRaw);
            String category = AqiCalculator.categoryFor(forecastAqi);
            String color = AqiCalculator.colorFor(forecastAqi);

            points.add(new ForecastResponse.ForecastPointDto(forecastTime.toInstant(), hour, forecastAqi, category, color));
            entityPoints.add(ForecastPoint.builder()
                    .forecastTime(forecastTime.toInstant()).hourOfDay(hour).aqi(forecastAqi)
                    .category(category).color(color).build());
        }

        int peakAqi = points.stream().mapToInt(ForecastResponse.ForecastPointDto::aqi).max().orElse(currentAqi);
        Instant peakTime = points.stream()
                .filter(p -> p.aqi() == peakAqi).findFirst().map(ForecastResponse.ForecastPointDto::time)
                .orElse(baseTime.toInstant());
        int avgAqi = (int) Math.round(points.stream().mapToInt(ForecastResponse.ForecastPointDto::aqi).average().orElse(currentAqi));

        Forecast entity = Forecast.builder()
                .region(region).lat(lat).lon(lon).currentAqi(currentAqi)
                .peakAqi(peakAqi).peakTime(peakTime).avgAqi(avgAqi)
                .generatedAt(Instant.now()).expiresAt(Instant.now().plus(Duration.ofHours(1)))
                .build();
        entityPoints.forEach(entity::addPoint);
        forecastRepository.save(entity);

        return new ForecastResponse(lat, lon, region, currentAqi, peakAqi, peakTime, avgAqi, points, entity.getGeneratedAt());
    }

    /** Deterministic pseudo-random seed derived the same way as the original hashlib.md5(...) % 1000 / 1000. */
    private static double seedFraction(double lat, double lon, int hourOffset) {
        try {
            String input = lat + "" + lon + "" + hourOffset;
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(input.getBytes(StandardCharsets.UTF_8));
            java.math.BigInteger bigInt = new java.math.BigInteger(1, digest);
            return bigInt.mod(java.math.BigInteger.valueOf(1000)).doubleValue() / 1000.0;
        } catch (Exception e) {
            return 0.5;
        }
    }
}
