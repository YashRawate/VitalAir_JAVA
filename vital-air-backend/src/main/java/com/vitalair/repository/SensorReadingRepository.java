package com.vitalair.repository;

import com.vitalair.entity.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    List<SensorReading> findByRegionAndRecordedAtAfter(String region, Instant since);

    @Query("""
           select s from SensorReading s
           where s.region = :region
             and s.recordedAt = (
                 select max(s2.recordedAt) from SensorReading s2
                 where s2.locationName = s.locationName and s2.region = :region
             )
           """)
    List<SensorReading> findLatestPerLocation(@Param("region") String region);

    @Query(value = """
            select * from sensor_readings s
            where s.recorded_at > :since
            order by (power(s.lat - :lat, 2) + power(s.lon - :lon, 2)) asc
            limit :limit
            """, nativeQuery = true)
    List<SensorReading> findNearest(@Param("lat") double lat,
                                     @Param("lon") double lon,
                                     @Param("since") Instant since,
                                     @Param("limit") int limit);

    void deleteByRecordedAtBefore(Instant cutoff);
}
