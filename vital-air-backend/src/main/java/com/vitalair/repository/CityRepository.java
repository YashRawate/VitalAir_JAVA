package com.vitalair.repository;

import com.vitalair.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CityRepository extends JpaRepository<City, Long> {
    List<City> findByRegion(String region);

    List<City> findByNameContainingIgnoreCase(String query);
}
