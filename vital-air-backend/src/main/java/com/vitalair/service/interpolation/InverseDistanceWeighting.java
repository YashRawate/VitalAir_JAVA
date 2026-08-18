package com.vitalair.service.interpolation;

import com.vitalair.util.HaversineUtil;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Inverse Distance Weighting. Direct port of idw.py's IDW implementation
 * (power=2 default), now actually reachable from the live prediction path.
 */
@Component
public class InverseDistanceWeighting implements InterpolationStrategy {

    private static final double POWER = 2.0;
    private static final double EPSILON = 1e-6;

    @Override
    public double interpolate(double queryLat, double queryLon, List<KnownPoint> knownPoints) {
        if (knownPoints.isEmpty()) {
            return Double.NaN;
        }

        double weightedSum = 0.0;
        double weightTotal = 0.0;

        for (KnownPoint p : knownPoints) {
            double distance = HaversineUtil.distanceKm(queryLat, queryLon, p.lat(), p.lon());
            if (distance < EPSILON) {
                return p.value(); // essentially on top of a known point
            }
            double weight = 1.0 / Math.pow(distance, POWER);
            weightedSum += weight * p.value();
            weightTotal += weight;
        }

        return weightTotal == 0 ? Double.NaN : weightedSum / weightTotal;
    }

    @Override
    public String name() {
        return "idw";
    }
}
