package com.vitalair.util;

import java.util.List;

/**
 * EPA breakpoint-based AQI calculator. Reimplements the piecewise-linear
 * concentration -&gt; AQI conversion used in main.py's calculate_aqi_from_pm25
 * (and the analogous PM10/CO/O3/NO2/SO2 helpers), consolidated into one
 * generic breakpoint table walker instead of six near-duplicate functions.
 */
public final class AqiCalculator {

    private AqiCalculator() {
    }

    public record Breakpoint(double concLow, double concHigh, int aqiLow, int aqiHigh) {
    }

    // US EPA breakpoints, µg/m³ unless noted.
    private static final List<Breakpoint> PM25 = List.of(
            new Breakpoint(0.0, 12.0, 0, 50),
            new Breakpoint(12.1, 35.4, 51, 100),
            new Breakpoint(35.5, 55.4, 101, 150),
            new Breakpoint(55.5, 150.4, 151, 200),
            new Breakpoint(150.5, 250.4, 201, 300),
            new Breakpoint(250.5, 350.4, 301, 400),
            new Breakpoint(350.5, 500.4, 401, 500)
    );

    private static final List<Breakpoint> PM10 = List.of(
            new Breakpoint(0, 54, 0, 50),
            new Breakpoint(55, 154, 51, 100),
            new Breakpoint(155, 254, 101, 150),
            new Breakpoint(255, 354, 151, 200),
            new Breakpoint(355, 424, 201, 300),
            new Breakpoint(425, 504, 301, 400),
            new Breakpoint(505, 604, 401, 500)
    );

    // CO in ppm
    private static final List<Breakpoint> CO = List.of(
            new Breakpoint(0.0, 4.4, 0, 50),
            new Breakpoint(4.5, 9.4, 51, 100),
            new Breakpoint(9.5, 12.4, 101, 150),
            new Breakpoint(12.5, 15.4, 151, 200),
            new Breakpoint(15.5, 30.4, 201, 300),
            new Breakpoint(30.5, 40.4, 301, 400),
            new Breakpoint(40.5, 50.4, 401, 500)
    );

    // O3 8-hr in ppb
    private static final List<Breakpoint> O3 = List.of(
            new Breakpoint(0, 54, 0, 50),
            new Breakpoint(55, 70, 51, 100),
            new Breakpoint(71, 85, 101, 150),
            new Breakpoint(86, 105, 151, 200),
            new Breakpoint(106, 200, 201, 300)
    );

    // NO2 in ppb
    private static final List<Breakpoint> NO2 = List.of(
            new Breakpoint(0, 53, 0, 50),
            new Breakpoint(54, 100, 51, 100),
            new Breakpoint(101, 360, 101, 150),
            new Breakpoint(361, 649, 151, 200),
            new Breakpoint(650, 1249, 201, 300),
            new Breakpoint(1250, 1649, 301, 400),
            new Breakpoint(1650, 2049, 401, 500)
    );

    // SO2 in ppb
    private static final List<Breakpoint> SO2 = List.of(
            new Breakpoint(0, 35, 0, 50),
            new Breakpoint(36, 75, 51, 100),
            new Breakpoint(76, 185, 101, 150),
            new Breakpoint(186, 304, 151, 200),
            new Breakpoint(305, 604, 201, 300)
    );

    private static int fromBreakpoints(double concentration, List<Breakpoint> table) {
        if (concentration <= 0) {
            return 0;
        }
        for (Breakpoint bp : table) {
            if (concentration >= bp.concLow() && concentration <= bp.concHigh()) {
                double aqi = ((bp.aqiHigh() - bp.aqiLow()) / (double) (bp.concHigh() - bp.concLow()))
                        * (concentration - bp.concLow()) + bp.aqiLow();
                return (int) Math.round(aqi);
            }
        }
        // Above the top breakpoint: clamp to the max index published, matching main.py's behaviour.
        Breakpoint top = table.get(table.size() - 1);
        return concentration > top.concHigh() ? 500 : top.aqiHigh();
    }

    public static int fromPm25(double ugm3) {
        return fromBreakpoints(ugm3, PM25);
    }

    public static int fromPm10(double ugm3) {
        return fromBreakpoints(ugm3, PM10);
    }

    public static int fromCo(double ppm) {
        return fromBreakpoints(ppm, CO);
    }

    public static int fromO3(double ppb) {
        return fromBreakpoints(ppb, O3);
    }

    public static int fromNo2(double ppb) {
        return fromBreakpoints(ppb, NO2);
    }

    public static int fromSo2(double ppb) {
        return fromBreakpoints(ppb, SO2);
    }

    /** Overall AQI is the max of the individual pollutant sub-indices (EPA convention). */
    public static int overallAqi(Double pm25, Double pm10, Double no2, Double co, Double o3, Double so2) {
        int max = 0;
        if (pm25 != null) max = Math.max(max, fromPm25(pm25));
        if (pm10 != null) max = Math.max(max, fromPm10(pm10));
        if (no2 != null) max = Math.max(max, fromNo2(no2));
        if (co != null) max = Math.max(max, fromCo(co));
        if (o3 != null) max = Math.max(max, fromO3(o3));
        if (so2 != null) max = Math.max(max, fromSo2(so2));
        return max;
    }

    public static String categoryFor(int aqi) {
        if (aqi <= 50) return "Good";
        if (aqi <= 100) return "Moderate";
        if (aqi <= 150) return "Unhealthy for Sensitive Groups";
        if (aqi <= 200) return "Unhealthy";
        if (aqi <= 300) return "Very Unhealthy";
        return "Hazardous";
    }

    public static String colorFor(int aqi) {
        if (aqi <= 50) return "#00e400";
        if (aqi <= 100) return "#ffff00";
        if (aqi <= 150) return "#ff7e00";
        if (aqi <= 200) return "#ff0000";
        if (aqi <= 300) return "#8f3f97";
        return "#7e0023";
    }
}
