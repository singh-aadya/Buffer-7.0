package com.smartagri.service;

import com.smartagri.model.Edge;
import com.smartagri.model.RouteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ============================================================
 * SERVICE: SimulationService
 * ============================================================
 * Handles dynamic simulation of real-world events that affect
 * the supply chain network (traffic, weather, floods, blockages).
 *
 * HOW IT WORKS:
 *   1. Modifies edge weights in GraphService dynamically
 *   2. Re-runs Dijkstra via RouteService with updated graph
 *   3. Returns new RouteResult showing impact of the event
 *
 * DSA INSIGHT:
 *   Dynamic graph updates demonstrate that Dijkstra must be
 *   RE-RUN after edge weight changes — there's no efficient
 *   way to incrementally update shortest paths when weights change.
 *
 *   This is why real-world systems (Google Maps) continuously
 *   re-compute routes as traffic conditions change.
 *
 * EVENT TYPES AND THEIR EFFECTS:
 *   TRAFFIC:  time multiplier 1.5x–2.5x (congestion)
 *   FLOOD:    blocks specific edges (sets impassable flag)
 *   WEATHER:  time 1.2x + carbon 1.1x (slower, engine works harder)
 *   BLOCKADE: blocks specific edges (road closure/protest)
 *   FESTIVAL: time 1.3x (seasonal slowdown)
 * ============================================================
 */
@Service
public class SimulationService {

    @Autowired
    private GraphService graphService;

    @Autowired
    private RouteService routeService;

    // -------------------------------------------------------
    // Track active events for display in the UI
    // Map: "fromId-toId" → event description
    // -------------------------------------------------------
    private final Map<String, String> activeEvents = new HashMap<>();

    /**
     * Applies a simulation event to specific edges in the graph.
     * Then re-runs Dijkstra to find the new optimal route.
     *
     * @param eventType     Type of event (TRAFFIC, FLOOD, WEATHER, BLOCKADE, FESTIVAL)
     * @param affectedEdges List of "fromId-toId" strings identifying affected edges
     * @param sourceId      Source node for re-routing
     * @param destId        Destination node for re-routing
     * @param multiplier    Weight multiplier to apply (1.0 = no change, 2.0 = doubled)
     * @return Updated RouteResult after simulation
     */
    public RouteResult applyEvent(String eventType, List<String> affectedEdges,
                                  String sourceId, String destId, double multiplier) {

        // -------------------------------------------------------
        // Step 1: Apply event to specified edges
        // Modifies the live graph — all future Dijkstra runs see updated weights
        // -------------------------------------------------------
        String eventDesc = getEventDescription(eventType, multiplier);

        for (String edgeKey : affectedEdges) {
            // Edge key format: "fromNodeId-toNodeId"
            String[] parts = edgeKey.split("-", 2);
            if (parts.length != 2) continue;

            String fromId = parts[0];
            String toId = parts[1];

            // Find the edge in the graph's adjacency list
            Edge edge = graphService.findEdge(fromId, toId);
            if (edge == null) {
                continue; // Edge doesn't exist — skip gracefully
            }

            // Apply event-specific modifications to edge weights
            applyEventToEdge(edge, eventType, multiplier);
            edge.setActiveEvent(eventDesc);

            // Track this as an active event
            activeEvents.put(edgeKey, eventDesc);
        }

        // -------------------------------------------------------
        // Step 2: Re-run Dijkstra with the updated graph
        //
        // DSA NOTE: We must run Dijkstra from scratch because
        // changing edge weights invalidates all previous shortest paths.
        // There's no O(1) way to update — full re-computation is required.
        // -------------------------------------------------------
        RouteResult newRoute = routeService.findOptimalRoute(sourceId, destId);
        newRoute.setHasActiveEvents(true);

        return newRoute;
    }

    /**
     * Applies event-specific modifications to an edge's weights.
     *
     * Design pattern: We modify the eventMultiplier and blocked flag,
     * NOT the base weights directly. This allows easy "undo" by
     * resetting multiplier to 1.0 and blocked to false.
     */
    private void applyEventToEdge(Edge edge, String eventType, double multiplier) {
        switch (eventType.toUpperCase()) {
            case "TRAFFIC":
                // Traffic increases travel time, and slightly increases carbon
                // (stop-and-go driving burns more fuel)
                edge.setEventMultiplier(multiplier);  // e.g., 1.5x–2.0x
                break;

            case "FLOOD":
                // Flood completely blocks the road — mark as impassable
                // Dijkstra will skip this edge (effective weight = MAX_VALUE)
                edge.setBlocked(true);
                edge.setActiveEvent("FLOOD: Road blocked");
                break;

            case "BLOCKADE":
                // Road closure due to protest or maintenance
                edge.setBlocked(true);
                edge.setActiveEvent("BLOCKADE: Route closed");
                break;

            case "WEATHER":
                // Bad weather (rain/fog) increases time AND carbon
                // Carbon increases because engines work harder in adverse conditions
                edge.setEventMultiplier(multiplier);  // Apply to all metrics
                break;

            case "FESTIVAL":
                // Seasonal congestion (harvest festivals, Diwali, etc.)
                // Increases time but not cost or carbon significantly
                edge.setEventMultiplier(multiplier);  // e.g., 1.3x
                break;

            default:
                // Generic event: apply multiplier to all weights
                edge.setEventMultiplier(multiplier);
        }
    }

    /**
     * Resets all simulation events — restores graph to original state.
     * Sets all edge multipliers back to 1.0 and unblocks all edges.
     */
    public void resetAllEvents() {
        // Reset all edges in the graph
        for (Edge edge : graphService.getAllEdges()) {
            edge.setEventMultiplier(1.0);    // No multiplier
            edge.setBlocked(false);           // Unblock
            edge.setActiveEvent(null);        // Clear event label
        }
        activeEvents.clear(); // Clear event tracking map
    }

    /**
     * Gets a human-readable description for an event type.
     */
    private String getEventDescription(String eventType, double multiplier) {
        return switch (eventType.toUpperCase()) {
            case "TRAFFIC"  -> String.format("TRAFFIC: %.0f%% slower", (multiplier - 1) * 100);
            case "FLOOD"    -> "FLOOD: Road blocked";
            case "BLOCKADE" -> "BLOCKADE: Road closed";
            case "WEATHER"  -> String.format("WEATHER: %.0f%% delay", (multiplier - 1) * 100);
            case "FESTIVAL" -> "FESTIVAL: Seasonal congestion";
            default         -> String.format("EVENT: x%.1f weight", multiplier);
        };
    }

    /**
     * Returns all currently active events for UI display.
     */
    public Map<String, String> getActiveEvents() {
        return Collections.unmodifiableMap(activeEvents);
    }

    /**
     * Applies a preset scenario for quick demo purposes.
     * These are pre-configured event combinations for common situations.
     */
    public RouteResult applyPresetScenario(String scenario, String sourceId, String destId) {
        resetAllEvents(); // Start fresh

        return switch (scenario.toUpperCase()) {
            case "MONSOON" -> {
                // Heavy rain scenario: multiple routes affected
                List<String> affectedEdges = Arrays.asList(
                    "FARM_PUNE-WH_PUNE",
                    "WH_PUNE-WH_MUMBAI",
                    "FARM_NASHIK-WH_PUNE"
                );
                yield applyEvent("WEATHER", affectedEdges, sourceId, destId, 1.4);
            }
            case "HIGHWAY_BLOCK" -> {
                // Highway blocked — forces village road alternative
                List<String> affectedEdges = Arrays.asList(
                    "WH_PUNE-WH_MUMBAI",
                    "COLD_PUNE-WH_MUMBAI"
                );
                yield applyEvent("BLOCKADE", affectedEdges, sourceId, destId, 1.0);
            }
            case "PEAK_TRAFFIC" -> {
                // Morning rush hour scenario
                List<String> affectedEdges = Arrays.asList(
                    "DC_THANE-MARKET_DADAR",
                    "WH_MUMBAI-MARKET_DADAR",
                    "DC_THANE-MARKET_VASHI"
                );
                yield applyEvent("TRAFFIC", affectedEdges, sourceId, destId, 2.0);
            }
            default -> routeService.findOptimalRoute(sourceId, destId);
        };
    }
}