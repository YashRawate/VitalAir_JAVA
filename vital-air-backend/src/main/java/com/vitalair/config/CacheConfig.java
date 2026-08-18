package com.vitalair.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Replaces the two in-memory Python dict caches (sensor_cache, 5 min TTL;
 * forecast_cache, 1 hr TTL) with proper bounded Caffeine caches.
 */
@Configuration
public class CacheConfig {

    public static final String SENSOR_CACHE = "sensorCache";
    public static final String FORECAST_CACHE = "forecastCache";

    @Bean
    public CacheManagerCustomizer<CaffeineCacheManager> caffeineCacheManagerCustomizer() {
        return cacheManager -> {
            cacheManager.setCaffeine(Caffeine.newBuilder().maximumSize(5_000));
            cacheManager.registerCustomCache(SENSOR_CACHE,
                    Caffeine.newBuilder()
                            .expireAfterWrite(5, TimeUnit.MINUTES)
                            .maximumSize(2_000)
                            .build());
            cacheManager.registerCustomCache(FORECAST_CACHE,
                    Caffeine.newBuilder()
                            .expireAfterWrite(1, TimeUnit.HOURS)
                            .maximumSize(2_000)
                            .build());
        };
    }
}
