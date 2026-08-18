package com.vitalair.controller;

import com.vitalair.dto.AqiPredictionResponse;
import com.vitalair.service.PredictionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/predict")
public class AqiController {

    private final PredictionService predictionService;

    public AqiController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @GetMapping("/{lat}/{lon}")
    public AqiPredictionResponse predict(@PathVariable double lat, @PathVariable double lon) {
        return predictionService.predict(lat, lon);
    }
}
