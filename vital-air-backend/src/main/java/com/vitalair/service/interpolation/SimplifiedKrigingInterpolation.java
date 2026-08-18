package com.vitalair.service.interpolation;

import com.vitalair.util.HaversineUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified ordinary-kriging-style interpolation using a spherical
 * semivariogram model. Port of the simplified kriging path from
 * grid_generator.py - like RBF, this existed in the original codebase but
 * was never reachable from a live endpoint. "Simplified" because it uses a
 * fixed-parameter semivariogram rather than fitting one per request, which
 * is a reasonable trade-off for a request-time API call.
 */
@Component
public class SimplifiedKrigingInterpolation implements InterpolationStrategy {

    private static final double RANGE_KM = 15.0; // distance at which spatial correlation plateaus
    private static final double SILL = 1.0;      // total variance at the range
    private static final double NUGGET = 0.05;   // measurement-noise floor

    @Override
    public double interpolate(double queryLat, double queryLon, List<KnownPoint> knownPoints) {
        int n = knownPoints.size();
        if (n == 0) {
            return Double.NaN;
        }
        if (n == 1) {
            return knownPoints.get(0).value();
        }

        // Semivariogram-derived weights, normalized (a lightweight stand-in
        // for solving the full kriging system, which is overkill for the
        // sparse point counts this API deals with per request).
        List<Double> weights = new ArrayList<>(n);
        double weightTotal = 0.0;

        for (KnownPoint p : knownPoints) {
            double distance = HaversineUtil.distanceKm(queryLat, queryLon, p.lat(), p.lon());
            double gamma = sphericalSemivariogram(distance);
            double weight = 1.0 / (gamma + 1e-6);
            weights.add(weight);
            weightTotal += weight;
        }

        double result = 0.0;
        for (int i = 0; i < n; i++) {
            result += (weights.get(i) / weightTotal) * knownPoints.get(i).value();
        }
        return result;
    }

    private double sphericalSemivariogram(double distance) {
        if (distance <= 0) {
            return NUGGET;
        }
        if (distance >= RANGE_KM) {
            return NUGGET + SILL;
        }
        double ratio = distance / RANGE_KM;
        return NUGGET + SILL * (1.5 * ratio - 0.5 * Math.pow(ratio, 3));
    }

    @Override
    public String name() {
        return "kriging";
    }
}
