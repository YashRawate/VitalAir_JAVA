package com.vitalair.controller;

import com.vitalair.service.SchedulerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SchedulerService schedulerService;

    public AdminController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @PostMapping("/collect-now")
    public List<String> collectNow() {
        return schedulerService.runCollectionNow();
    }
}
