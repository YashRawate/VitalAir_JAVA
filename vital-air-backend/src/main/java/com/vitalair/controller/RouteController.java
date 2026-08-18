package com.vitalair.controller;

import com.vitalair.dto.RouteResponse;
import com.vitalair.service.RouteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/route")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping("/safe")
    public RouteResponse safeRoute(@RequestParam("start_lat") double startLat,
                                    @RequestParam("start_lon") double startLon,
                                    @RequestParam("end_lat") double endLat,
                                    @RequestParam("end_lon") double endLon) {
        return routeService.safeRoute(startLat, startLon, endLat, endLon);
    }
}
