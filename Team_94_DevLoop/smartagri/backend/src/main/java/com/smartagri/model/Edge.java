package com.smartagri.model;

/**
 * ============================================================
 * MODEL: Edge
 * ============================================================
 * Represents a directed road/route (edge) between two nodes
 * in the agricultural supply chain graph.

 * WHY DIRECTED EDGES:
 * - Real roads may have different costs/times in each direction
 * - Allows modeling one-way roads or asymmetric terrain

 * MULTI-OBJECTIVE WEIGHTS:
 * This edge holds three independent weights for the scoring formula:
 *   score = Wc*cost + Wt*time + Wco2*carbon
 *
 * These weights are dynamically modified during simulation
 * (e.g., traffic increases time, floods block routes by setting cost=∞).

 * SPACE COMPLEXITY: O(E) total for all edges, where E = number of edges
 * ============================================================
 */
public class Edge {

    // Source and destination node IDs for graph traversal
    private String fromNodeId;
    private String toNodeId;

    // -------------------------------------------------------
    // WEIGHT 1: Transport cost in INR (Indian Rupees)
    // Includes fuel, toll, driver wages per route
    // Used in: score formula as Wc * cost
    // -------------------------------------------------------
    private double cost;

    // -------------------------------------------------------
    // WEIGHT 2: Travel time in hours
    // Affected by: distance, road type, traffic
    // Used in: score formula as Wt * time
    // -------------------------------------------------------
    private double timeHours;

    // -------------------------------------------------------
    // WEIGHT 3: Carbon emissions in kg CO2
    // Calculated from: distance * vehicle emission factor
    // Used in: score formula as Wco2 * carbon
    // -------------------------------------------------------
    private double carbonKg;

    // Distance in kilometers (used for carbon calculation reference)
    private double distanceKm;

    // Road type affects base cost and emissions multiplier
    // Values: HIGHWAY, STATE_ROAD, VILLAGE_ROAD, FERRY
    private String roadType;

    // -------------------------------------------------------
    // Dynamic event modifier — multiplied to weights during simulation.
    // Default = 1.0 (no modification).
    // Traffic: 1.5x time, Flood: set cost to MAX (blocked),
    // Weather: 1.2x time + 1.1x carbon
    // -------------------------------------------------------
    private double eventMultiplier = 1.0;

    // Flag to mark edge as blocked (impassable)
    // Blocked edges are skipped during Dijkstra relaxation
    private boolean blocked = false;

    // Active event description for UI display
    private String activeEvent = null;

    // Default constructor
    public Edge() {}

    public Edge(String fromNodeId, String toNodeId, double cost, double timeHours,
                double carbonKg, double distanceKm, String roadType) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.cost = cost;
        this.timeHours = timeHours;
        this.carbonKg = carbonKg;
        this.distanceKm = distanceKm;
        this.roadType = roadType;
    }

    // -------------------------------------------------------
    // EFFECTIVE WEIGHT METHODS:
    // These return dynamically adjusted values after applying
    // the event multiplier. Dijkstra uses these, NOT raw values.
    // This design separates base data from runtime simulation state.
    // -------------------------------------------------------

    /** Returns cost adjusted for active events */
    public double getEffectiveCost() {
        if (blocked) return Double.MAX_VALUE / 3; // Practically infinite, avoid overflow
        return cost * eventMultiplier;
    }

    /** Returns time adjusted for active events */
    public double getEffectiveTime() {
        if (blocked) return Double.MAX_VALUE / 3;
        return timeHours * eventMultiplier;
    }

    /** Returns carbon adjusted for active events */
    public double getEffectiveCarbon() {
        if (blocked) return Double.MAX_VALUE / 3;
        return carbonKg * eventMultiplier;
    }

    @Override
    public String toString() {
        return "Edge{" + fromNodeId + " -> " + toNodeId +
                ", cost=" + cost + ", time=" + timeHours + "h, carbon=" + carbonKg + "kg}";
    }

    // ===================== Getters & Setters =====================

    public String getFromNodeId() { return fromNodeId; }
    public void setFromNodeId(String fromNodeId) { this.fromNodeId = fromNodeId; }

    public String getToNodeId() { return toNodeId; }
    public void setToNodeId(String toNodeId) { this.toNodeId = toNodeId; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public double getTimeHours() { return timeHours; }
    public void setTimeHours(double timeHours) { this.timeHours = timeHours; }

    public double getCarbonKg() { return carbonKg; }
    public void setCarbonKg(double carbonKg) { this.carbonKg = carbonKg; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public String getRoadType() { return roadType; }
    public void setRoadType(String roadType) { this.roadType = roadType; }

    public double getEventMultiplier() { return eventMultiplier; }
    public void setEventMultiplier(double eventMultiplier) { this.eventMultiplier = eventMultiplier; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public String getActiveEvent() { return activeEvent; }
    public void setActiveEvent(String activeEvent) { this.activeEvent = activeEvent; }
}