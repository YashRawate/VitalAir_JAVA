package com.vitalair.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "vitalair.jwt")
public class JwtProperties {
    /** Base64 or plain secret - set via VITALAIR_JWT_SECRET env var in production. */
    private String secret;
    private long expirationMs = 86_400_000L; // 24h
}
