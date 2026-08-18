package com.vitalair.controller;

import com.vitalair.dto.HeatmapResponse;
import com.vitalair.service.HeatmapService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/heatmap")
public class HeatmapController {

    private final HeatmapService heatmapService;

    public HeatmapController(HeatmapService heatmapService) {
        this.heatmapService = heatmapService;
    }

    @GetMapping
    public HeatmapResponse heatmap(@RequestParam(defaultValue = "delhi") String region) {
        return heatmapService.heatmap(region);
    }
}
