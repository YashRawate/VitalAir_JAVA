package com.vitalair.controller;

import com.vitalair.dto.ForecastResponse;
import com.vitalair.service.ForecastService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping
    public ForecastResponse forecast(@RequestParam double lat,
                                      @RequestParam double lon,
                                      @RequestParam(defaultValue = "24") int hours) {
        return forecastService.forecast(lat, lon, hours);
    }
}
