package com.vitalair.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Persisted air-quality reading for a location. Replaces the DynamoDB
 * "air-quality-sensors" / "air-quality-historical" tables written by the
 * original data_collector.py Lambda.
 */
@Entity
@Table(name = "sensor_readings", indexes = {
        @Index(name = "idx_sensor_lat_lon", columnList = "lat,lon"),
        @Index(name = "idx_sensor_region_recorded", columnList = "region,recordedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String locationName;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lon;

    @Column(length = 30)
    private String region;

    private Double pm25;
    private Double pm10;
    private Double no2;
    private Double co;
    private Double o3;
    private Double so2;

    private Integer aqi;

    private Double temperature;
    private Double humidity;
    private Double windSpeed;
    private Double windDirection;
    private Double pressure;

    private Double congestionRatio;
    private Integer nearbyFireCount;

    @Column(length = 30)
    private String source;

    @Column(nullable = false)
    private Instant recordedAt;

    @PrePersist
    void onCreate() {
        if (this.recordedAt == null) {
            this.recordedAt = Instant.now();
        }
    }
}
