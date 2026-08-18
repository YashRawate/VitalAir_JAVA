package com.vitalair.repository;

import com.vitalair.entity.Forecast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ForecastRepository extends JpaRepository<Forecast, Long> {
    Optional<Forecast> findTopByRegionAndLatAndLonAndExpiresAtAfterOrderByGeneratedAtDesc(
            String region, Double lat, Double lon, Instant now);

    List<Forecast> findByRegionOrderByGeneratedAtDesc(String region);

    void deleteByExpiresAtBefore(Instant cutoff);
}
