package com.vitalair.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Static reference city used for hotspot base-values, search-locations, and
 * heatmap seeding. Loaded from application.yml at startup (see
 * vitalair.regions.*.cities) rather than hardcoded arrays as in main.py.
 */
@Entity
@Table(name = "cities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lon;

    @Column(nullable = false, length = 30)
    private String region;

    @Column(length = 30)
    private String type;

    private Integer baseAqi;
}
