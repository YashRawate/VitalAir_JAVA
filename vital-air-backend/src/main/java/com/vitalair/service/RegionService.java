package com.vitalair.service;

import com.vitalair.config.RegionProperties;
import com.vitalair.util.HaversineUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Region/city helpers. Region + city data are bound from application.yml
 * (vitalair.regions.*) instead of the hardcoded REGIONS / CITIES dicts in
 * main.py, but the lookup logic (bounding-box containment, nearest-city)
 * is unchanged.
 */
@Slf4j
@Service
public class RegionService {

    private final RegionProperties regionProperties;

    public RegionService(RegionProperties regionProperties) {
        this.regionProperties = regionProperties;
    }

    public Map<String, RegionProperties.Region> regions() {
        return regionProperties.getRegions();
    }

    public Optional<RegionProperties.Region> region(String key) {
        return Optional.ofNullable(regionProperties.getRegions().get(key));
    }

    /** Port of get_region_from_coords(lat, lon): bounding-box containment check. */
    public String regionFromCoords(double lat, double lon) {
        for (var entry : regionProperties.getRegions().entrySet()) {
            RegionProperties.Region r = entry.getValue();
            if (lat >= r.getLatMin() && lat <= r.getLatMax() && lon >= r.getLonMin() && lon <= r.getLonMax()) {
                return entry.getKey();
            }
        }
        return null;
    }

    public List<RegionProperties.CityDef> citiesFor(String region) {
        RegionProperties.Region r = regionProperties.getRegions().get(region);
        return r == null ? List.of() : r.getCities();
    }

    /** Nearest city + distance, port of the loop in /predict. */
    public NearestCity nearestCity(double lat, double lon, String region) {
        RegionProperties.CityDef nearest = null;
        double minDist = Double.POSITIVE_INFINITY;
        for (RegionProperties.CityDef city : citiesFor(region)) {
            double dist = HaversineUtil.distanceKm(lat, lon, city.getLat(), city.getLon());
            if (dist < minDist) {
                minDist = dist;
                nearest = city;
            }
        }
        return nearest == null ? null : new NearestCity(nearest.getName(), minDist);
    }

    public record NearestCity(String name, double distanceKm) {
    }
}
