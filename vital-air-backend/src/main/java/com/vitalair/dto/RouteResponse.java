package com.vitalair.dto;

import java.util.List;

public record RouteResponse(
        RouteOption direct,
        RouteOption safe,
        double exposureReductionPct,
        double distanceIncreasePct
) {
    public record RouteOption(
            List<double[]> waypoints,
            double distanceKm,
            int avgAqi,
            int maxAqi
    ) {
    }
}
