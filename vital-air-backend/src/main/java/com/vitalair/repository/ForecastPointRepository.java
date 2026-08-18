package com.vitalair.repository;

import com.vitalair.entity.ForecastPoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForecastPointRepository extends JpaRepository<ForecastPoint, Long> {
}
