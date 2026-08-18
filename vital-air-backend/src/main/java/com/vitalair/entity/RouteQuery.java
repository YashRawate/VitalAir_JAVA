package com.vitalair.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Audit/history record for a "safe route" comparison, i.e. the direct route
 * vs. the AQI-optimized route between two points. New in the Java version -
 * the original prototype computed this on the fly and never persisted it.
 */
@Entity
@Table(name = "route_queries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Double startLat;
    private Double startLon;
    private Double endLat;
    private Double endLon;

    private Double directDistanceKm;
    private Integer directAvgAqi;

    private Double safeDistanceKm;
    private Integer safeAvgAqi;

    private Double exposureReductionPct;
    private Double distanceIncreasePct;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
