package com.vitalair.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Stored short-term AQI forecast for a location. Replaces DynamoDB
 * "air-quality-forecasts" table + S3 forecast JSON blobs.
 */
@Entity
@Table(name = "forecasts", indexes = {
        @Index(name = "idx_forecast_region", columnList = "region,generatedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Forecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String region;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lon;

    private Integer currentAqi;

    private Integer peakAqi;
    private Instant peakTime;
    private Integer avgAqi;

    @OneToMany(mappedBy = "forecast", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ForecastPoint> points = new ArrayList<>();

    @Column(nullable = false)
    private Instant generatedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @PrePersist
    void onCreate() {
        if (this.generatedAt == null) {
            this.generatedAt = Instant.now();
        }
    }

    public void addPoint(ForecastPoint point) {
        point.setForecast(this);
        this.points.add(point);
    }
}
