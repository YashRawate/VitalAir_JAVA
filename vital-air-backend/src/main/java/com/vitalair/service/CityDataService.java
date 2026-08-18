package com.vitalair.service;

import com.vitalair.config.RegionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Port of fetch_all_cities_data(region): pulls current AQI for every
 * reference city in a region, falling back to the city's static base_aqi
 * (scaled into rough pollutant estimates) when every upstream API fails -
 * exactly as main.py did, so a total outage still yields a usable response.
 */
@Slf4j
@Service
public class CityDataService {

    private final RegionService regionService;
    private final AqiFetchService aqiFetchService;

    public CityDataService(RegionService regionService, AqiFetchService aqiFetchService) {
        this.regionService = regionService;
        this.aqiFetchService = aqiFetchService;
    }

    public record CityAqi(String name, double lat, double lon, String type, int aqi,
                           Double pm25, Double pm10, Double no2, Double co, Double o3,
                           long timestampEpochSeconds, String source) {
    }

    public List<CityAqi> fetchAllCitiesData(String region) {
        List<RegionProperties.CityDef> cities = regionService.citiesFor(region);
        List<CityAqi> result = new ArrayList<>();

        for (RegionProperties.CityDef city : cities) {
            AqiReading reading = null;
            try {
                reading = aqiFetchService.fetchAnyAqi(city.getLat(), city.getLon());
            } catch (Exception e) {
                log.warn("Failed fetching AQI for city {}: {}", city.getName(), e.getMessage());
            }

            if (reading != null) {
                result.add(new CityAqi(city.getName(), city.getLat(), city.getLon(), city.getType(),
                        reading.aqi(), reading.pm25(), reading.pm10(), reading.no2(), reading.co(), reading.o3(),
                        reading.timestampEpochSeconds(), reading.source()));
                log.info("Got data for {} from {}", city.getName(), reading.source());
            } else {
                int base = city.getBaseAqi();
                result.add(new CityAqi(city.getName(), city.getLat(), city.getLon(), city.getType(),
                        base, round(base / 2.0), round(base * 0.8), round(base * 0.3), round(base * 0.01),
                        round(base * 0.4), Instant.now().getEpochSecond(), "base_data"));
                log.info("Using base data for {}", city.getName());
            }
        }
        return result;
    }

    private static Double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
