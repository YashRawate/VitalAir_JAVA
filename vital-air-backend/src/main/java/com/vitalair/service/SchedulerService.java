package com.vitalair.service;

import com.vitalair.entity.SensorReading;
import com.vitalair.repository.SensorReadingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Replaces the two independent AWS Lambdas from the original project:
 *
 *  - data_collector.py (hourly CloudWatch-triggered): pulled live AQI/weather
 *    for a fixed list of cities and wrote to DynamoDB + S3.
 *  - ml_processor.py (ran after the collector): built an interpolated grid
 *    from the freshly collected readings.
 *
 * Both become plain @Scheduled jobs in the same Spring Boot process, per the
 * scoping decision for this migration - simpler to deploy than mirroring two
 * separate Lambdas, at the cost of coupling collection to app uptime.
 */
@Slf4j
@Service
public class SchedulerService {

    private final RegionService regionService;
    private final AqiFetchService aqiFetchService;
    private final SensorReadingRepository sensorReadingRepository;

    public SchedulerService(RegionService regionService, AqiFetchService aqiFetchService,
                             SensorReadingRepository sensorReadingRepository) {
        this.regionService = regionService;
        this.aqiFetchService = aqiFetchService;
        this.sensorReadingRepository = sensorReadingRepository;
    }

    /** Collector equivalent: every hour, on the hour. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void collectSensorData() {
        log.info("Scheduled data collection starting");
        int written = 0;

        for (var entry : regionService.regions().entrySet()) {
            String regionKey = entry.getKey();
            for (var city : entry.getValue().getCities()) {
                try {
                    AqiReading reading = aqiFetchService.fetchAnyAqi(city.getLat(), city.getLon());
                    if (reading == null) {
                        continue;
                    }
                    sensorReadingRepository.save(SensorReading.builder()
                            .locationName(city.getName()).lat(city.getLat()).lon(city.getLon()).region(regionKey)
                            .pm25(reading.pm25()).pm10(reading.pm10()).no2(reading.no2())
                            .co(reading.co()).o3(reading.o3()).so2(reading.so2()).aqi(reading.aqi())
                            .source(reading.source()).recordedAt(Instant.now())
                            .build());
                    written++;
                } catch (Exception e) {
                    log.warn("Collection failed for {}: {}", city.getName(), e.getMessage());
                }
            }
        }
        log.info("Scheduled data collection complete - {} readings written", written);
    }

    /**
     * Processor equivalent: runs shortly after collection. In this
     * architecture the "processing" step is just cache warmth / cleanup -
     * actual grid interpolation happens on-demand in HeatmapService against
     * the freshly collected rows, avoiding a second large batch job.
     */
    @Scheduled(cron = "0 15 * * * *")
    @Transactional
    public void pruneOldReadings() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        sensorReadingRepository.deleteByRecordedAtBefore(cutoff);
        log.info("Pruned sensor readings older than {}", cutoff);
    }

    /** Manual trigger for admins (see AdminController) - same job, on demand. */
    public List<String> runCollectionNow() {
        collectSensorData();
        return List.of("Collection triggered");
    }
}
