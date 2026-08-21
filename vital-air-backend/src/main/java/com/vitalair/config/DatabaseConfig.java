package com.vitalair.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Production database configuration.
 * Automatically parses cloud database URIs (Render, Railway, Heroku)
 * formatted as postgres://user:pass@host:port/db and converts them into
 * standard JDBC URLs (jdbc:postgresql://host:port/db).
 */
@Slf4j
@Configuration
@Profile("prod")
public class DatabaseConfig {

    @Value("${spring.datasource.url:${DB_URL:${DATABASE_URL:}}}")
    private String dbUrl;

    @Value("${spring.datasource.username:${DB_USERNAME:}}")
    private String username;

    @Value("${spring.datasource.password:${DB_PASSWORD:}}")
    private String password;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");

        String formattedUrl = dbUrl;
        String extractedUser = username;
        String extractedPass = password;

        if (formattedUrl != null && !formattedUrl.isBlank()) {
            if (formattedUrl.startsWith("postgres://") || formattedUrl.startsWith("postgresql://")) {
                try {
                    URI uri = new URI(formattedUrl);
                    String host = uri.getHost();
                    int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                    String path = uri.getPath();

                    if (uri.getUserInfo() != null) {
                        String[] userInfo = uri.getUserInfo().split(":");
                        if (extractedUser == null || extractedUser.isBlank()) {
                            extractedUser = userInfo[0];
                        }
                        if (userInfo.length > 1 && (extractedPass == null || extractedPass.isBlank())) {
                            extractedPass = userInfo[1];
                        }
                    }

                    formattedUrl = "jdbc:postgresql://" + host + ":" + port + path;
                    log.info("Successfully formatted database URL to JDBC: jdbc:postgresql://{}:{}{}", host, port, path);
                } catch (Exception e) {
                    log.warn("Failed to parse database URI {}, falling back to raw string: {}", dbUrl, e.getMessage());
                }
            } else if (!formattedUrl.startsWith("jdbc:")) {
                formattedUrl = "jdbc:" + formattedUrl;
            }
        }

        config.setJdbcUrl(formattedUrl);
        if (extractedUser != null && !extractedUser.isBlank()) {
            config.setUsername(extractedUser);
        }
        if (extractedPass != null && !extractedPass.isBlank()) {
            config.setPassword(extractedPass);
        }

        return new HikariDataSource(config);
    }
}
