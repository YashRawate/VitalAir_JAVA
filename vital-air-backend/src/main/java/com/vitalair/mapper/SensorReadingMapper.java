package com.vitalair.mapper;

import com.vitalair.dto.SensorResponse;
import com.vitalair.entity.SensorReading;
import com.vitalair.util.AqiCalculator;
import org.springframework.stereotype.Component;

/**
 * Entity -> DTO mapping for sensor readings. Kept as a plain hand-written
 * mapper rather than MapStruct-generated, since the transformation also
 * needs to derive category/color from the raw AQI value, not just copy
 * fields 1:1.
 */
@Component
public class SensorReadingMapper {

    public SensorResponse toResponse(SensorReading entity) {
        int aqi = entity.getAqi() == null ? 0 : entity.getAqi();
        return new SensorResponse(
                entity.getLocationName(),
                entity.getLat(),
                entity.getLon(),
                aqi,
                AqiCalculator.categoryFor(aqi),
                AqiCalculator.colorFor(aqi),
                entity.getPm25(),
                entity.getSource(),
                entity.getRecordedAt()
        );
    }
}
