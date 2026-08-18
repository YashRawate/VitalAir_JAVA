package com.vitalair.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A single AQI severity ring/zone for a region (Zone 1 - Good ... Zone 6 -
 * Hazardous). Polygon points are stored as JSON text; Postgres users can
 * migrate this column to native geometry/PostGIS later if needed.
 */
@Entity
@Table(name = "aqi_zones", indexes = {
        @Index(name = "idx_zone_region", columnList = "region,generatedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AqiZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String region;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(length = 20)
    private String aqiRange;

    @Column(length = 10)
    private String color;

    private Double radiusKm;

    private Double centerLat;
    private Double centerLon;

    @Lob
    @Column(name = "polygon_points_json")
    private String polygonPointsJson;

    private Integer cityCount;

    @Column(nullable = false)
    private Instant generatedAt;

    @PrePersist
    void onCreate() {
        if (this.generatedAt == null) {
            this.generatedAt = Instant.now();
        }
    }
}
