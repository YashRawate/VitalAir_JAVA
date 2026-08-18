package com.vitalair.service;

import com.vitalair.config.RegionProperties;
import com.vitalair.dto.HotspotResponse;
import com.vitalair.dto.LocationSearchResult;
import com.vitalair.dto.PollutionSourceResponse;
import com.vitalair.util.AqiCalculator;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Port of /search-locations, /hotspots, and /pollution-sources. */
@Service
public class LocationService {

    private static final Set<Integer> DELHI_WINTER_MONTHS = Set.of(10, 11, 12, 1);

    private final RegionService regionService;
    private final CityDataService cityDataService;

    public LocationService(RegionService regionService, CityDataService cityDataService) {
        this.regionService = regionService;
        this.cityDataService = cityDataService;
    }

    public List<LocationSearchResult> searchLocations(String query) {
        String q = query.toLowerCase().trim();
        List<LocationSearchResult> results = new ArrayList<>();

        for (var entry : regionService.regions().entrySet()) {
            String regionKey = entry.getKey();
            RegionProperties.Region region = entry.getValue();
            for (RegionProperties.CityDef city : region.getCities()) {
                if (city.getName().toLowerCase().contains(q)) {
                    results.add(new LocationSearchResult(city.getName(), city.getLat(), city.getLon(),
                            regionKey, city.getType()));
                }
            }
        }
        return results.size() > 10 ? results.subList(0, 10) : results;
    }

    public List<HotspotResponse> hotspots(String region) {
        List<CityDataService.CityAqi> cityData = cityDataService.fetchAllCitiesData(region);
        List<HotspotResponse> hotspots = new ArrayList<>();
        for (CityDataService.CityAqi city : cityData) {
            if ("hotspot".equals(city.type()) || city.aqi() >= 150) {
                hotspots.add(new HotspotResponse(city.name(), city.lat(), city.lon(), city.aqi(),
                        AqiCalculator.categoryFor(city.aqi()), city.type()));
            }
        }
        return hotspots;
    }

    /** Port of /pollution-sources: seasonal breakdown for Delhi, static breakdown elsewhere. */
    public List<PollutionSourceResponse> pollutionSources(String region) {
        if ("delhi".equals(region)) {
            int month = LocalDate.now().getMonthValue();
            if (DELHI_WINTER_MONTHS.contains(month)) {
                return List.of(
                        new PollutionSourceResponse("vehicle", 0, 0, "Vehicle Emissions", 35),
                        new PollutionSourceResponse("biomass", 0, 0, "Biomass Burning", 30),
                        new PollutionSourceResponse("industrial", 0, 0, "Industrial", 20),
                        new PollutionSourceResponse("construction", 0, 0, "Construction Dust", 10),
                        new PollutionSourceResponse("other", 0, 0, "Others", 5)
                );
            }
            return List.of(
                    new PollutionSourceResponse("vehicle", 0, 0, "Vehicle Emissions", 45),
                    new PollutionSourceResponse("industrial", 0, 0, "Industrial", 25),
                    new PollutionSourceResponse("construction", 0, 0, "Construction Dust", 15),
                    new PollutionSourceResponse("biomass", 0, 0, "Biomass Burning", 10),
                    new PollutionSourceResponse("other", 0, 0, "Others", 5)
            );
        }
        return List.of(
                new PollutionSourceResponse("industrial", 0, 0, "Industrial", 40),
                new PollutionSourceResponse("vehicle", 0, 0, "Vehicle Emissions", 30),
                new PollutionSourceResponse("construction", 0, 0, "Construction Dust", 15),
                new PollutionSourceResponse("other", 0, 0, "Others", 15)
        );
    }
}
