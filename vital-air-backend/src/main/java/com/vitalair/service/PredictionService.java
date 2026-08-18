package com.vitalair.service;

import com.vitalair.dto.AqiPredictionResponse;
import com.vitalair.entity.PredictionHistory;
import com.vitalair.exception.UpstreamDataException;
import com.vitalair.repository.PredictionHistoryRepository;
import com.vitalair.util.AqiCalculator;
import org.springframework.stereotype.Service;

import java.time.Instant;

/** Port of the /predict/{lat}/{lon} endpoint in main.py. */
@Service
public class PredictionService {

    private final AqiFetchService aqiFetchService;
    private final RegionService regionService;
    private final PredictionHistoryRepository predictionHistoryRepository;

    public PredictionService(AqiFetchService aqiFetchService, RegionService regionService,
                              PredictionHistoryRepository predictionHistoryRepository) {
        this.aqiFetchService = aqiFetchService;
        this.regionService = regionService;
        this.predictionHistoryRepository = predictionHistoryRepository;
    }

    public AqiPredictionResponse predict(double lat, double lon) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Invalid coordinates");
        }

        AqiReading aqiData = aqiFetchService.fetchAnyAqi(lat, lon);
        WeatherReading weather = aqiFetchService.fetchAnyWeather(lat, lon);

        if (aqiData == null) {
            throw new UpstreamDataException("No air quality data available from any API. Please try again later.");
        }

        int aqi = aqiData.aqi() > 0 ? aqiData.aqi() : 100;
        String category = AqiCalculator.categoryFor(aqi);
        String color = AqiCalculator.colorFor(aqi);

        String region = regionService.regionFromCoords(lat, lon);
        RegionService.NearestCity nearest = region == null ? null : regionService.nearestCity(lat, lon, region);

        int confidence = 95;
        if ("openmeteo".equals(aqiData.source())) {
            confidence = 80;
        } else if ("base_data".equals(aqiData.source())) {
            confidence = 70;
        }
        if (nearest != null && nearest.distanceKm() > 20) {
            confidence = 60;
        }

        predictionHistoryRepository.save(PredictionHistory.builder()
                .lat(lat).lon(lon).aqi(aqi).category(category).source(aqiData.source())
                .confidence(confidence).createdAt(Instant.now())
                .build());

        return new AqiPredictionResponse(
                round4(lat), round4(lon), aqi, category, color, aqiData.source(), confidence,
                aqiData.pm25(), aqiData.pm10(), aqiData.no2(), aqiData.co(), aqiData.o3(), aqiData.so2(),
                weather != null ? weather.temperature() : null,
                weather != null ? weather.humidity() : null,
                weather != null ? weather.windSpeed() : null
        );
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
