package com.smartagri.model;
import java.util.List;

/**
 * ============================================================
 * MODEL: RouteResult
 * ============================================================
 * Encapsulates the complete output of a Dijkstra routing computation.
 * Returned by RouteService after running the optimization algorithm.
 *
 * Contains:
 * - The optimal path (ordered list of Node objects)
 * - Total metrics: cost, time, carbon
 * - Multi-objective score
 * - Comparison with naive (baseline) route for impact visualization
 * - Eco points earned from carbon reduction
 *
 * WHY SEPARATE RESULT MODEL:
 * Separation of concerns — the algorithm (RouteService) computes the
 * path; this model holds the result for serialization to the frontend.
 * ============================================================
 */
public class RouteResult {

    // Ordered sequence of nodes from source to destination
    // Built by backtracking through the parentMap in Dijkstra
    private List<Node> path;

    // Ordered sequence of edges used along the path
    private List<Edge> edges;

    // -------------------------------------------------------
    // AGGREGATE METRICS along the optimized path:
    // Sum of effective weights across all edges in the path
    // -------------------------------------------------------
    private double totalCostINR;     // Total transport cost in rupees
    private double totalTimeHours;   // Total travel time in hours
    private double totalCarbonKg;    // Total CO2 emissions in kilograms

    // -------------------------------------------------------
    // MULTI-OBJECTIVE SCORE:
    // score = Wc*cost + Wt*time + Wco2*carbon
    // Lower score = better route
    // Weights used: Wc=0.4, Wt=0.3, Wco2=0.3 (configurable)
    // -------------------------------------------------------
    private double multiObjectiveScore;
    private double weightCost;
    private double weightTime;
    private double weightCarbon;

    // -------------------------------------------------------
    // BASELINE COMPARISON (naive/non-optimized route):
    // The "before" metrics for demonstrating optimization impact.
    // Baseline = shortest path by distance only (no optimization).
    // -------------------------------------------------------
    private double baselineCostINR;
    private double baselineTimeHours;
    private double baselineCarbonKg;

    // -------------------------------------------------------
    // IMPROVEMENT PERCENTAGES:
    // ((baseline - optimized) / baseline) * 100
    // -------------------------------------------------------
    private double costSavedPercent;
    private double timeReducedPercent;
    private double carbonReducedPercent;

    // -------------------------------------------------------
    // ECO POINTS:
    // Gamification metric to incentivize green routing.
    // Formula: ecoPoints = carbonReducedPercent * 10
    // Displayed as "+XX Eco Points" in the UI
    // -------------------------------------------------------
    private int ecoPoints;

    // Human-readable path description (e.g., "Farm A → Warehouse B → Market C")
    private String pathDescription;

    // Total path distance in kilometers
    private double totalDistanceKm;

    // Algorithm that produced this result
    private String algorithmUsed = "Dijkstra's Algorithm (Multi-Objective)";

    // Whether this result was computed with active simulation events
    private boolean hasActiveEvents = false;

    // Source and destination node IDs
    private String sourceNodeId;
    private String destinationNodeId;

    // Error message if route not found
    private boolean routeFound = true;
    private String errorMessage;

    // Default constructor
    public RouteResult() {}

    // ===================== Getters & Setters =====================

    public List<Node> getPath() { return path; }
    public void setPath(List<Node> path) { this.path = path; }

    public List<Edge> getEdges() { return edges; }
    public void setEdges(List<Edge> edges) { this.edges = edges; }

    public double getTotalCostINR() { return totalCostINR; }
    public void setTotalCostINR(double totalCostINR) { this.totalCostINR = totalCostINR; }

    public double getTotalTimeHours() { return totalTimeHours; }
    public void setTotalTimeHours(double totalTimeHours) { this.totalTimeHours = totalTimeHours; }

    public double getTotalCarbonKg() { return totalCarbonKg; }
    public void setTotalCarbonKg(double totalCarbonKg) { this.totalCarbonKg = totalCarbonKg; }

    public double getMultiObjectiveScore() { return multiObjectiveScore; }
    public void setMultiObjectiveScore(double multiObjectiveScore) { this.multiObjectiveScore = multiObjectiveScore; }

    public double getWeightCost() { return weightCost; }
    public void setWeightCost(double weightCost) { this.weightCost = weightCost; }

    public double getWeightTime() { return weightTime; }
    public void setWeightTime(double weightTime) { this.weightTime = weightTime; }

    public double getWeightCarbon() { return weightCarbon; }
    public void setWeightCarbon(double weightCarbon) { this.weightCarbon = weightCarbon; }

    public double getBaselineCostINR() { return baselineCostINR; }
    public void setBaselineCostINR(double baselineCostINR) { this.baselineCostINR = baselineCostINR; }

    public double getBaselineTimeHours() { return baselineTimeHours; }
    public void setBaselineTimeHours(double baselineTimeHours) { this.baselineTimeHours = baselineTimeHours; }

    public double getBaselineCarbonKg() { return baselineCarbonKg; }
    public void setBaselineCarbonKg(double baselineCarbonKg) { this.baselineCarbonKg = baselineCarbonKg; }

    public double getCostSavedPercent() { return costSavedPercent; }
    public void setCostSavedPercent(double costSavedPercent) { this.costSavedPercent = costSavedPercent; }

    public double getTimeReducedPercent() { return timeReducedPercent; }
    public void setTimeReducedPercent(double timeReducedPercent) { this.timeReducedPercent = timeReducedPercent; }

    public double getCarbonReducedPercent() { return carbonReducedPercent; }
    public void setCarbonReducedPercent(double carbonReducedPercent) { this.carbonReducedPercent = carbonReducedPercent; }

    public int getEcoPoints() { return ecoPoints; }
    public void setEcoPoints(int ecoPoints) { this.ecoPoints = ecoPoints; }

    public String getPathDescription() { return pathDescription; }
    public void setPathDescription(String pathDescription) { this.pathDescription = pathDescription; }

    public double getTotalDistanceKm() { return totalDistanceKm; }
    public void setTotalDistanceKm(double totalDistanceKm) { this.totalDistanceKm = totalDistanceKm; }

    public String getAlgorithmUsed() { return algorithmUsed; }
    public void setAlgorithmUsed(String algorithmUsed) { this.algorithmUsed = algorithmUsed; }

    public boolean isHasActiveEvents() { return hasActiveEvents; }
    public void setHasActiveEvents(boolean hasActiveEvents) { this.hasActiveEvents = hasActiveEvents; }

    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId; }

    public String getDestinationNodeId() { return destinationNodeId; }
    public void setDestinationNodeId(String destinationNodeId) { this.destinationNodeId = destinationNodeId; }

    public boolean isRouteFound() { return routeFound; }
    public void setRouteFound(boolean routeFound) { this.routeFound = routeFound; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
} 
