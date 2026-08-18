package com.vitalair.controller;

import com.vitalair.dto.HotspotResponse;
import com.vitalair.dto.LocationSearchResult;
import com.vitalair.dto.PollutionSourceResponse;
import com.vitalair.service.LocationService;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/api/locations/search")
    public List<LocationSearchResult> search(@RequestParam @Size(min = 2) String query) {
        return locationService.searchLocations(query);
    }

    @GetMapping("/api/hotspots")
    public List<HotspotResponse> hotspots(@RequestParam(defaultValue = "delhi") String region) {
        return locationService.hotspots(region);
    }

    @GetMapping("/api/pollution-sources")
    public List<PollutionSourceResponse> pollutionSources(@RequestParam(defaultValue = "delhi") String region) {
        return locationService.pollutionSources(region);
    }
}
