package com.vitalair.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Third-party API credentials.
 *
 * IMPORTANT: unlike the original main.py (which had real-looking keys hardcoded
 * as literal fallback defaults), every value here comes ONLY from environment
 * variables / application.yml. No literal key ever belongs in source control.
 * See .env.example for the variables this expects.
 */
@Data
@ConfigurationProperties(prefix = "vitalair.api-keys")
public class ApiKeysProperties {
    private String openweather;
    private String tomtom;
    private String nasaFirms;
    private String openaq;
}
