package com.vitalair.service;

import com.vitalair.dto.RouteResponse;
import com.vitalair.entity.RouteQuery;
import com.vitalair.exception.UpstreamDataException;
import com.vitalair.repository.RouteQueryRepository;
import com.vitalair.util.HaversineUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of /safe-route: generates a direct path between two points, then
 * nudges each waypoint toward whichever nearby grid cell has lower
 * IDW-estimated AQI (same 5x5 grid search and offsets as the original).
 */
@Service
public class RouteService {

    private static final double[] OFFSETS = {-0.00018, -0.00009, 0, 0.00009, 0.00018};

    private final AqiFetchService aqiFetchService;
    private final RegionService regionService;
    private final CityDataService cityDataService;
    private final RouteQueryRepository routeQueryRepository;

    public RouteService(AqiFetchService aqiFetchService, RegionService regionService,
                         CityDataService cityDataService, RouteQueryRepository routeQueryRepository) {
        this.aqiFetchService = aqiFetchService;
        this.regionService = regionService;
        this.cityDataService = cityDataService;
        this.routeQueryRepository = routeQueryRepository;
    }

    @Transactional
    public RouteResponse safeRoute(double startLat, double startLon, double endLat, double endLon) {
        AqiReading startData = aqiFetchService.fetchAnyAqi(startLat, startLon);
        AqiReading endData = aqiFetchService.fetchAnyAqi(endLat, endLon);

        if (startData == null || endData == null) {
            throw new UpstreamDataException("Cannot calculate route without sufficient AQI data");
        }

        double startAqi = startData.aqi() > 0 ? startData.aqi() : 150;
        double endAqi = endData.aqi() > 0 ? endData.aqi() : 150;

        String region = regionService.regionFromCoords(startLat, startLon);
        List<CityDataService.CityAqi> cityData = cityDataService.fetchAllCitiesData(region == null ? "delhi" : region);

        int steps = Math.max(20, (int) (HaversineUtil.distanceKm(startLat, startLon, endLat, endLon) * 50));
        steps = Math.min(steps, 100);

        List<double[]> directPath = new ArrayList<>();
        List<Double> directAqi = new ArrayList<>();
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double lat = startLat + (endLat - startLat) * t;
            double lon = startLon + (endLon - startLon) * t;
            directPath.add(new double[]{round6(lat), round6(lon)});
            directAqi.add(startAqi * (1 - t) + endAqi * t);
        }

        double directDistance = HaversineUtil.distanceKm(startLat, startLon, endLat, endLon);
        double directAvgAqi = directAqi.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        List<double[]> safePath = new ArrayList<>();
        List<Double> safeAqi = new ArrayList<>();

        for (int i = 0; i < directPath.size(); i++) {
            double lat = directPath.get(i)[0];
            double lon = directPath.get(i)[1];
            double bestAqi = directAqi.get(i);
            double[] bestPoint = {lat, lon};

            for (double dLat : OFFSETS) {
                for (double dLon : OFFSETS) {
                    if (dLat == 0 && dLon == 0) continue;
                    double testLat = lat + dLat;
                    double testLon = lon + dLon;

                    Double testAqi = idwEstimate(testLat, testLon, cityData);
                    if (testAqi != null && testAqi < bestAqi) {
                        bestAqi = testAqi;
                        bestPoint = new double[]{testLat, testLon};
                    }
                }
            }
            safePath.add(new double[]{round6(bestPoint[0]), round6(bestPoint[1])});
            safeAqi.add(bestAqi);
        }

        double safeDistance = 0;
        for (int i = 1; i < safePath.size(); i++) {
            safeDistance += HaversineUtil.distanceKm(
                    safePath.get(i - 1)[0], safePath.get(i - 1)[1], safePath.get(i)[0], safePath.get(i)[1]);
        }
        double safeAvgAqi = safeAqi.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        double exposureReduction = directAvgAqi > 0 ? ((directAvgAqi - safeAvgAqi) / directAvgAqi) * 100 : 0;
        double distanceIncrease = directDistance > 0 ? ((safeDistance - directDistance) / directDistance) * 100 : 0;

        routeQueryRepository.save(RouteQuery.builder()
                .startLat(startLat).startLon(startLon).endLat(endLat).endLon(endLon)
                .directDistanceKm(round2(directDistance)).directAvgAqi((int) Math.round(directAvgAqi))
                .safeDistanceKm(round2(safeDistance)).safeAvgAqi((int) Math.round(safeAvgAqi))
                .exposureReductionPct(round1(exposureReduction)).distanceIncreasePct(round1(distanceIncrease))
                .build());

        RouteResponse.RouteOption direct = new RouteResponse.RouteOption(
                directPath, round2(directDistance), (int) Math.round(directAvgAqi),
                (int) Math.round(directAqi.stream().mapToDouble(Double::doubleValue).max().orElse(directAvgAqi)));
        RouteResponse.RouteOption safe = new RouteResponse.RouteOption(
                safePath, round2(safeDistance), (int) Math.round(safeAvgAqi),
                (int) Math.round(safeAqi.stream().mapToDouble(Double::doubleValue).max().orElse(safeAvgAqi)));

        return new RouteResponse(direct, safe, round1(exposureReduction), round1(distanceIncrease));
    }

    /** Inline IDW against the region's reference cities, matching the original's grid-search estimator. */
    private Double idwEstimate(double lat, double lon, List<CityDataService.CityAqi> cityData) {
        if (cityData.isEmpty()) {
            return null;
        }
        double weightedSum = 0;
        double totalWeight = 0;
        for (CityDataService.CityAqi city : cityData) {
            double dist = HaversineUtil.distanceKm(lat, lon, city.lat(), city.lon());
            if (dist < 0.02) {
                return (double) city.aqi();
            }
            double weight = dist > 0 ? 1.0 / (dist * dist) : 1.0;
            weightedSum += weight * city.aqi();
            totalWeight += weight;
        }
        return totalWeight > 0 ? weightedSum / totalWeight : null;
    }

    private static double round6(double v) {
        return Math.round(v * 1_000_000.0) / 1_000_000.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
