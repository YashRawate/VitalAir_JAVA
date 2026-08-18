package com.vitalair.service.interpolation;

import com.vitalair.entity.SensorReading;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Facade over the pluggable interpolation strategies, plus the temporal-decay
 * and fire/traffic-influence blending that ml_processor.py applied on top of
 * raw spatial interpolation when building its heatmap grid.
 */
@Service
public class InterpolationService {

    private final Map<String, InterpolationStrategy> strategies;

    public InterpolationService(List<InterpolationStrategy> strategyBeans) {
        this.strategies = strategyBeans.stream()
                .collect(Collectors.toMap(InterpolationStrategy::name, s -> s));
    }

    /** Default strategy for most callers - IDW is what the original prototype effectively used. */
    public double interpolate(double lat, double lon, List<SensorReading> readings) {
        return interpolate(lat, lon, readings, "idw");
    }

    public double interpolate(double lat, double lon, List<SensorReading> readings, String strategyName) {
        InterpolationStrategy strategy = strategies.getOrDefault(strategyName, strategies.get("idw"));
        List<InterpolationStrategy.KnownPoint> points = readings.stream()
                .filter(r -> r.getAqi() != null)
                .map(r -> new InterpolationStrategy.KnownPoint(r.getLat(), r.getLon(), r.getAqi()))
                .toList();
        return strategy.interpolate(lat, lon, points);
    }

    /**
     * Applies an exponential recency-decay weight before interpolating, matching
     * ml_processor.py's preference for fresher readings when blending the grid.
     * Older readings are pulled toward the fresh-reading mean rather than
     * discarded outright, so a single stale sensor doesn't wildly skew a cell
     * that has no recent data at all.
     */
    public double interpolateWithTemporalDecay(double lat, double lon, List<SensorReading> readings,
                                                String strategyName, double halfLifeHours) {
        Instant now = Instant.now();
        InterpolationStrategy strategy = strategies.getOrDefault(strategyName, strategies.get("idw"));

        List<SensorReading> valid = readings.stream().filter(r -> r.getAqi() != null).toList();
        if (valid.isEmpty()) {
            return Double.NaN;
        }

        double freshMean = valid.stream()
                .mapToDouble(SensorReading::getAqi)
                .average()
                .orElse(0);

        List<InterpolationStrategy.KnownPoint> decayedPoints = valid.stream()
                .map(r -> {
                    double ageHours = Duration.between(r.getRecordedAt(), now).toMinutes() / 60.0;
                    double decay = Math.exp(-Math.log(2) * ageHours / halfLifeHours); // 1.0 = fresh, ->0 = stale
                    double blendedValue = r.getAqi() * decay + freshMean * (1 - decay);
                    return new InterpolationStrategy.KnownPoint(r.getLat(), r.getLon(), blendedValue);
                })
                .toList();

        return strategy.interpolate(lat, lon, decayedPoints);
    }

    public List<String> availableStrategies() {
        return strategies.keySet().stream().sorted().toList();
    }
}
