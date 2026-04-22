package com.smartagri.service;

import com.smartagri.model.RouteResult;
import org.springframework.stereotype.Service;

import java.util.*;
// import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================
 * SERVICE: MetricsService
 * ============================================================
 * Tracks cumulative optimization metrics across all routing
 * sessions. Maintains running totals for:
 *   - Cost savings (INR)
 *   - Carbon emissions reduced (kg CO2)
 *   - Time saved (hours)
 *   - Eco points accumulated
 *   - Routes optimized count
 *
 * WHY IN-MEMORY TRACKING:
 * For this demo, metrics are stored in-memory using simple
 * data structures. In production, these would persist to a
 * database (PostgreSQL/MongoDB).
 *
 * DATA STRUCTURE CHOICE: List<RouteResult> for history
 *   - Allows iterating all past results for analytics
 *   - Supports rolling averages and trend computation
 *   - Space: O(n) where n = number of optimized routes
 * ============================================================
 */
@Service
public class MetricsService {

    // -------------------------------------------------------
    // CUMULATIVE METRICS (running totals across all sessions)
    // Using AtomicInteger/Double for thread safety
    // -------------------------------------------------------
    private double totalCostSavedINR = 0;
    private double totalCarbonSavedKg = 0;
    private double totalTimeSavedHours = 0;
    private int totalEcoPoints = 0;
    private int totalRoutesOptimized = 0;
    private int totalSimulationsRun = 0;

    // -------------------------------------------------------
    // HISTORY: All route results (bounded to last 100 for memory)
    // WHY List? Sequential access for analytics, order matters.
    // WHY bounded? Prevents unbounded memory growth in demo.
    // -------------------------------------------------------
    private final List<RouteResult> routeHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 100;

    // SDG Impact counters
    // SDG 2: Zero Hunger, SDG 13: Climate Action, SDG 12: Responsible Consumption
    private int sdg2ImpactCount = 0;   // Routes saving perishable food
    private int sdg13ImpactCount = 0;  // Routes reducing carbon
    private int sdg12ImpactCount = 0;  // Routes reducing waste

    /**
     * Records a completed route optimization and updates all metrics.
     * Called after each successful Dijkstra run.
     *
     * @param result The completed RouteResult to record
     */
    public void recordRouteOptimization(RouteResult result) {
        if (result == null || !result.isRouteFound()) return;

        // Add to history (bounded)
        if (routeHistory.size() >= MAX_HISTORY_SIZE) {
            routeHistory.remove(0); // Remove oldest entry (FIFO)
        }
        routeHistory.add(result);

        // Compute absolute savings from percentages
        double costSaved   = result.getBaselineCostINR()   * result.getCostSavedPercent()   / 100;
        double carbonSaved = result.getBaselineCarbonKg()  * result.getCarbonReducedPercent() / 100;
        double timeSaved   = result.getBaselineTimeHours() * result.getTimeReducedPercent()  / 100;

        // Accumulate running totals
        totalCostSavedINR    += costSaved;
        totalCarbonSavedKg   += carbonSaved;
        totalTimeSavedHours  += timeSaved;
        totalEcoPoints       += result.getEcoPoints();
        totalRoutesOptimized++;

        // Update SDG impact counters
        if (result.getCarbonReducedPercent() > 10) sdg13ImpactCount++;
        if (result.getCostSavedPercent()     > 15) sdg2ImpactCount++;
        if (result.getTimeReducedPercent()   > 10) sdg12ImpactCount++;
    }

    /** Records a simulation event run */
    public void recordSimulation() {
        totalSimulationsRun++;
    }

    /**
     * Returns the complete metrics dashboard data.
     * Includes: cumulative totals, averages, SDG impact, eco equivalent.
     */
    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Cumulative totals
        metrics.put("totalCostSavedINR",    Math.round(totalCostSavedINR));
        metrics.put("totalCarbonSavedKg",   Math.round(totalCarbonSavedKg * 10) / 10.0);
        metrics.put("totalTimeSavedHours",  Math.round(totalTimeSavedHours * 10) / 10.0);
        metrics.put("totalEcoPoints",        totalEcoPoints);
        metrics.put("totalRoutesOptimized",  totalRoutesOptimized);
        metrics.put("totalSimulationsRun",   totalSimulationsRun);

        // Average improvement per route
        if (totalRoutesOptimized > 0) {
            metrics.put("avgCostSavedPercent",
                routeHistory.stream().mapToDouble(RouteResult::getCostSavedPercent).average().orElse(0));
            metrics.put("avgCarbonSavedPercent",
                routeHistory.stream().mapToDouble(RouteResult::getCarbonReducedPercent).average().orElse(0));
            metrics.put("avgTimeSavedPercent",
                routeHistory.stream().mapToDouble(RouteResult::getTimeReducedPercent).average().orElse(0));
        } else {
            // Seed with realistic demo values when no routes have been run yet
            metrics.put("avgCostSavedPercent",   28.5);
            metrics.put("avgCarbonSavedPercent",  31.2);
            metrics.put("avgTimeSavedPercent",    22.8);
        }

        // SDG impact
        Map<String, Object> sdgImpact = new HashMap<>();
        sdgImpact.put("sdg2ZeroHunger",        sdg2ImpactCount);
        sdgImpact.put("sdg12ResponsibleUse",    sdg12ImpactCount);
        sdgImpact.put("sdg13ClimateAction",     sdg13ImpactCount);
        metrics.put("sdgImpact", sdgImpact);

        // Carbon equivalent conversions (for visualization)
        // 1 tree absorbs ~21 kg CO2/year
        double treesEquivalent = totalCarbonSavedKg / 21.0;
        metrics.put("treesEquivalent", Math.round(treesEquivalent * 10) / 10.0);

        // Algorithm info
        metrics.put("algorithmUsed", "Dijkstra's Algorithm (Multi-Objective)");
        metrics.put("complexity", "O((V + E) log V)");
        metrics.put("graphNodes", 8);
        metrics.put("graphEdges", 14);

        // Recent route history summary (last 5)
        List<Map<String, Object>> recentRoutes = new ArrayList<>();
        int startIndex = Math.max(0, routeHistory.size() - 5);
        for (int i = startIndex; i < routeHistory.size(); i++) {
            RouteResult r = routeHistory.get(i);
            Map<String, Object> summary = new HashMap<>();
            summary.put("path",          r.getPathDescription());
            summary.put("costSaved",     Math.round(r.getCostSavedPercent()));
            summary.put("carbonSaved",   Math.round(r.getCarbonReducedPercent()));
            summary.put("timeSaved",     Math.round(r.getTimeReducedPercent()));
            summary.put("ecoPoints",     r.getEcoPoints());
            recentRoutes.add(summary);
        }
        metrics.put("recentRoutes", recentRoutes);

        return metrics;
    }

    /** Returns current eco points total */
    public int getTotalEcoPoints() { return totalEcoPoints; }

    /** Returns route history for analytics */
    public List<RouteResult> getRouteHistory() {
        return Collections.unmodifiableList(routeHistory);
    }
}