package com.vitalair.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Audit trail of /predict lookups. New in the Java version - the original
 * prototype did not persist individual prediction requests.
 */
@Entity
@Table(name = "prediction_history", indexes = {
        @Index(name = "idx_prediction_created", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double lat;
    private Double lon;
    private Integer aqi;

    @Column(length = 40)
    private String category;

    @Column(length = 30)
    private String source;

    private Integer confidence;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
