package com.vitalair.service.interpolation;

import java.util.List;

/**
 * Strategy interface for spatial interpolation of AQI values from known
 * sensor points to an arbitrary query point. Replaces the single crude
 * inline IDW loop in the original main.py, and finally wires in the
 * additional methods (RBF, kriging) that existed in idw.py / grid_generator.py
 * but were dead code in the hackathon prototype.
 */
public interface InterpolationStrategy {

    record KnownPoint(double lat, double lon, double value) {
    }

    double interpolate(double queryLat, double queryLon, List<KnownPoint> knownPoints);

    String name();
}
