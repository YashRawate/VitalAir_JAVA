package com.vitalair.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "vitalair.external-apis")
public class ExternalApiProperties {
    private String openweatherAqiUrl = "https://api.openweathermap.org/data/2.5/air_pollution";
    private String openweatherWeatherUrl = "https://api.openweathermap.org/data/2.5/weather";
    private String openaqUrl = "https://api.openaq.org/v2/latest";
    private String openMeteoAqiUrl = "https://air-quality-api.open-meteo.com/v1/air-quality";
    private String openMeteoWeatherUrl = "https://api.open-meteo.com/v1/forecast";
    private int timeoutSeconds = 10;
}
