package com.vitalair.repository;

import com.vitalair.entity.AqiZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AqiZoneRepository extends JpaRepository<AqiZone, Long> {
    List<AqiZone> findByRegionOrderByLevelAsc(String region);

    List<AqiZone> findTop6ByRegionOrderByGeneratedAtDescLevelAsc(String region);

    void deleteByGeneratedAtBefore(Instant cutoff);
}
