package com.vitalair.service;

import com.vitalair.dto.SensorResponse;
import com.vitalair.mapper.SensorReadingMapper;
import com.vitalair.repository.SensorReadingRepository;
import com.vitalair.util.AqiCalculator;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/** Port of /sensors: latest reading per location for a region. */
@Service
public class SensorService {

    private final SensorReadingRepository sensorReadingRepository;
    private final CityDataService cityDataService;
    private final SensorReadingMapper sensorReadingMapper;

    public SensorService(SensorReadingRepository sensorReadingRepository, CityDataService cityDataService,
                          SensorReadingMapper sensorReadingMapper) {
        this.sensorReadingRepository = sensorReadingRepository;
        this.cityDataService = cityDataService;
        this.sensorReadingMapper = sensorReadingMapper;
    }

    public List<SensorResponse> sensors(String region) {
        List<SensorResponse> stored = sensorReadingRepository.findLatestPerLocation(region).stream()
                .map(sensorReadingMapper::toResponse)
                .toList();

        if (!stored.isEmpty()) {
            return stored;
        }

        // Fall back to a live fetch, same spirit as fetch_all_cities_data's base-data fallback.
        return cityDataService.fetchAllCitiesData(region).stream()
                .map(c -> new SensorResponse(c.name(), c.lat(), c.lon(), c.aqi(),
                        AqiCalculator.categoryFor(c.aqi()), AqiCalculator.colorFor(c.aqi()),
                        c.pm25(), c.source(), Instant.ofEpochSecond(c.timestampEpochSeconds())))
                .toList();
    }
}
