package com.vitalair;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Vital Air - Hyper-local Air Quality Intelligence Platform.
 *
 * Java/Spring Boot reimplementation of the original hackathon FastAPI + AWS Lambda
 * prototype ("Vital Air", TECHNEX'26 Eco-Hackathon). See README.md for full
 * architecture notes and an honest account of what carried over 1:1 versus what
 * was reworked during this migration.
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan
public class VitalAirApplication {
    public static void main(String[] args) {
        SpringApplication.run(VitalAirApplication.class, args);
    }
}
