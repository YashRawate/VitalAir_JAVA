package com.vitalair.service.interpolation;

import com.vitalair.util.HaversineUtil;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Radial Basis Function interpolation using a Gaussian kernel. Port of the
 * RBF path in idw.py that was implemented in the original project but never
 * wired into main.py's live endpoints.
 */
@Component
public class RadialBasisFunctionInterpolation implements InterpolationStrategy {

    /** Kernel width in km - controls how quickly influence decays with distance. */
    private static final double SIGMA_KM = 5.0;

    @Override
    public double interpolate(double queryLat, double queryLon, List<KnownPoint> knownPoints) {
        if (knownPoints.isEmpty()) {
            return Double.NaN;
        }

        double weightedSum = 0.0;
        double weightTotal = 0.0;

        for (KnownPoint p : knownPoints) {
            double distance = HaversineUtil.distanceKm(queryLat, queryLon, p.lat(), p.lon());
            double weight = Math.exp(-(distance * distance) / (2 * SIGMA_KM * SIGMA_KM));
            weightedSum += weight * p.value();
            weightTotal += weight;
        }

        return weightTotal == 0 ? Double.NaN : weightedSum / weightTotal;
    }

    @Override
    public String name() {
        return "rbf";
    }
}
