package com.vitalair.controller;

import com.vitalair.dto.SensorResponse;
import com.vitalair.service.SensorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @GetMapping
    public List<SensorResponse> sensors(@RequestParam(defaultValue = "delhi") String region) {
        return sensorService.sensors(region);
    }
}
