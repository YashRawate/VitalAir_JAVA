package com.vitalair.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vitalair.config.ApiKeysProperties;
import com.vitalair.config.CacheConfig;
import com.vitalair.config.ExternalApiProperties;
import com.vitalair.util.AqiCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Faithful port of main.py's multi-API fallback chain:
 * fetch_any_aqi() tries OpenWeather -> OpenAQ -> Open-Meteo in order and
 * returns the first usable result; fetch_any_weather() tries OpenWeather -> Open-Meteo.
 *
 * Unlike the original, API keys are only ever read from configuration
 * (environment variables), never hardcoded literals - see ApiKeysProperties.
 */
@Slf4j
@Service
public class AqiFetchService {

    private final RestClient restClient;
    private final ApiKeysProperties apiKeys;
    private final ExternalApiProperties endpoints;

    public AqiFetchService(RestClient restClient, ApiKeysProperties apiKeys, ExternalApiProperties endpoints) {
        this.restClient = restClient;
        this.apiKeys = apiKeys;
        this.endpoints = endpoints;
    }

    /** Equivalent of fetch_any_aqi(lat, lon). Cached for 5 minutes per rounded coordinate, like sensor_cache. */
    @Cacheable(value = CacheConfig.SENSOR_CACHE, key = "'aqi_' + T(java.lang.Math).round(#lat * 10000) + '_' + T(java.lang.Math).round(#lon * 10000)")
    public AqiReading fetchAnyAqi(double lat, double lon) {
        List<Map.Entry<String, BiFunction<Double, Double, AqiReading>>> apis = List.of(
                Map.entry("OpenWeather", this::fetchOpenWeatherAqi),
                Map.entry("OpenAQ", this::fetchOpenAqAqi),
                Map.entry("Open-Meteo", this::fetchOpenMeteoAqi)
        );

        for (var entry : apis) {
            try {
                log.info("Trying {} for {},{}", entry.getKey(), lat, lon);
                AqiReading result = entry.getValue().apply(lat, lon);
                if (result != null && (result.aqi() > 0 || result.pm25() != null)) {
                    log.info("Success with {} - AQI: {}", entry.getKey(), result.aqi());
                    return result;
                }
            } catch (Exception e) {
                log.warn("{} failed: {}", entry.getKey(), e.getMessage());
            }
        }
        log.error("All AQI APIs failed for {},{}", lat, lon);
        return null;
    }

    /** Equivalent of fetch_any_weather(lat, lon). */
    public WeatherReading fetchAnyWeather(double lat, double lon) {
        WeatherReading weather = fetchOpenWeatherWeather(lat, lon);
        if (weather != null) {
            return weather;
        }
        return fetchOpenMeteoWeather(lat, lon);
    }

    // --- OpenWeather --------------------------------------------------

    private AqiReading fetchOpenWeatherAqi(double lat, double lon) {
        if (apiKeys.getOpenweather() == null || apiKeys.getOpenweather().isBlank()) {
            return null;
        }
        try {
            JsonNode data = restClient.get()
                    .uri(endpoints.getOpenweatherAqiUrl() + "?lat={lat}&lon={lon}&appid={key}",
                            lat, lon, apiKeys.getOpenweather())
                    .retrieve()
                    .body(JsonNode.class);

            if (data == null || !data.has("list") || data.get("list").isEmpty()) {
                return null;
            }
            JsonNode first = data.get("list").get(0);
            JsonNode components = first.path("components");

            Double pm25 = nullableDouble(components, "pm2_5");
            Double pm10 = nullableDouble(components, "pm10");
            Double no2 = nullableDouble(components, "no2");
            Double so2 = nullableDouble(components, "so2");
            Double o3 = nullableDouble(components, "o3");
            Double coRaw = nullableDouble(components, "co");
            Double co = coRaw != null ? round(coRaw / 1000.0, 3) : null;

            int aqi = AqiCalculator.overallAqi(pm25, pm10, no2, co, o3, so2);
            long ts = first.path("dt").asLong(Instant.now().getEpochSecond());

            return new AqiReading(round(pm25, 1), round(pm10, 1), round(no2, 1), round(so2, 1), co, round(o3, 1),
                    aqi, ts, "openweather");
        } catch (Exception e) {
            log.warn("OpenWeather AQI error: {}", e.getMessage());
            return null;
        }
    }

    private WeatherReading fetchOpenWeatherWeather(double lat, double lon) {
        if (apiKeys.getOpenweather() == null || apiKeys.getOpenweather().isBlank()) {
            return null;
        }
        try {
            JsonNode data = restClient.get()
                    .uri(endpoints.getOpenweatherWeatherUrl() + "?lat={lat}&lon={lon}&appid={key}&units=metric",
                            lat, lon, apiKeys.getOpenweather())
                    .retrieve()
                    .body(JsonNode.class);

            if (data == null) {
                return null;
            }
            JsonNode main = data.path("main");
            JsonNode wind = data.path("wind");
            String condition = data.path("weather").isArray() && !data.path("weather").isEmpty()
                    ? data.path("weather").get(0).path("description").asText(null)
                    : null;

            return new WeatherReading(
                    nullableDouble(main, "temp"),
                    nullableDouble(main, "humidity"),
                    nullableDouble(main, "pressure"),
                    nullableDouble(wind, "speed"),
                    nullableDouble(wind, "deg"),
                    condition,
                    "openweather");
        } catch (Exception e) {
            log.warn("OpenWeather weather error: {}", e.getMessage());
            return null;
        }
    }

    // --- OpenAQ ---------------------------------------------------------

    private AqiReading fetchOpenAqAqi(double lat, double lon) {
        try {
            RestClient.RequestHeadersSpec<?> req = restClient.get()
                    .uri(endpoints.getOpenaqUrl() + "?coordinates={coords}&radius=25000&limit=1",
                            lat + "," + lon);
            if (apiKeys.getOpenaq() != null && !apiKeys.getOpenaq().isBlank()) {
                req = req.header("X-API-Key", apiKeys.getOpenaq());
            }
            JsonNode data = req.retrieve().body(JsonNode.class);

            if (data == null || !data.has("results") || data.get("results").isEmpty()) {
                return null;
            }
            JsonNode measurements = data.get("results").get(0).path("measurements");

            Double pm25 = null, pm10 = null, no2 = null, o3 = null, co = null;
            for (JsonNode m : measurements) {
                String parameter = m.path("parameter").asText("");
                double value = m.path("value").asDouble();
                switch (parameter) {
                    case "pm25" -> pm25 = value;
                    case "pm10" -> pm10 = value;
                    case "no2" -> no2 = value;
                    case "o3" -> o3 = value;
                    case "co" -> co = value / 1000.0;
                    default -> { /* ignore other pollutants, matches original */ }
                }
            }

            int aqi = AqiCalculator.overallAqi(pm25, pm10, no2, co, o3, null);
            return new AqiReading(round(pm25, 1), round(pm10, 1), round(no2, 1), null,
                    co != null ? round(co, 3) : null, round(o3, 1), aqi, Instant.now().getEpochSecond(), "openaq");
        } catch (Exception e) {
            log.warn("OpenAQ error: {}", e.getMessage());
            return null;
        }
    }

    // --- Open-Meteo (free fallback, no key required) --------------------

    private AqiReading fetchOpenMeteoAqi(double lat, double lon) {
        try {
            JsonNode data = restClient.get()
                    .uri(endpoints.getOpenMeteoAqiUrl()
                                    + "?latitude={lat}&longitude={lon}&current=pm2_5,pm10,nitrogen_dioxide,ozone,carbon_monoxide&timeformat=unixtime",
                            lat, lon)
                    .retrieve()
                    .body(JsonNode.class);

            if (data == null) {
                return null;
            }
            JsonNode current = data.path("current");

            Double pm25 = nullableDouble(current, "pm2_5");
            Double pm10 = nullableDouble(current, "pm10");
            Double no2 = nullableDouble(current, "nitrogen_dioxide");
            Double o3 = nullableDouble(current, "ozone");
            Double coUgm3 = nullableDouble(current, "carbon_monoxide");
            Double coPpm = coUgm3 != null ? round(coUgm3 / 1150.0, 3) : null;

            int aqi = AqiCalculator.overallAqi(pm25, pm10, no2, coPpm, o3, null);
            long ts = current.path("time").asLong(Instant.now().getEpochSecond());

            return new AqiReading(round(pm25, 1), round(pm10, 1), round(no2, 1), null, coPpm, round(o3, 1),
                    aqi, ts, "openmeteo");
        } catch (Exception e) {
            log.warn("Open-Meteo AQI error: {}", e.getMessage());
            return null;
        }
    }

    private WeatherReading fetchOpenMeteoWeather(double lat, double lon) {
        try {
            JsonNode data = restClient.get()
                    .uri(endpoints.getOpenMeteoWeatherUrl()
                                    + "?latitude={lat}&longitude={lon}&current=temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m,pressure_msl&timeformat=unixtime",
                            lat, lon)
                    .retrieve()
                    .body(JsonNode.class);

            if (data == null) {
                return null;
            }
            JsonNode current = data.path("current");
            return new WeatherReading(
                    nullableDouble(current, "temperature_2m"),
                    nullableDouble(current, "relative_humidity_2m"),
                    nullableDouble(current, "pressure_msl"),
                    nullableDouble(current, "wind_speed_10m"),
                    nullableDouble(current, "wind_direction_10m"),
                    null,
                    "openmeteo");
        } catch (Exception e) {
            log.warn("Open-Meteo weather error: {}", e.getMessage());
            return null;
        }
    }

    // --- helpers ----------------------------------------------------

    private static Double nullableDouble(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asDouble();
    }

    private static Double round(Double value, int places) {
        if (value == null) {
            return null;
        }
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }
}
