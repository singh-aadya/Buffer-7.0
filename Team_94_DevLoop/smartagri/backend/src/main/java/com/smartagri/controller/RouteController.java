package com.smartagri.controller;

import com.smartagri.model.RouteResult;
import com.smartagri.service.GraphService;
import com.smartagri.service.MetricsService;
import com.smartagri.service.RouteService;
import com.smartagri.service.SimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ============================================================
 * CONTROLLER: RouteController
 * ============================================================
 * REST API endpoints for the SmartAgri Routing Engine.
 *
 * ENDPOINTS:
 *   POST /route/optimize   - Run Dijkstra to find optimal route
 *   POST /simulate/event   - Apply simulation event and re-route
 *   GET  /simulate/reset   - Reset all simulation events
 *   GET  /metrics          - Get impact dashboard metrics
 *   GET  /graph            - Get graph structure for visualization
 *   GET  /graph/init       - Re-initialize default graph
 *   POST /graph/node       - Add a custom node
 *   POST /graph/edge       - Add a custom edge
 *   GET  /vehicles         - List available vehicles
 * ============================================================
 */
@RestController
@CrossOrigin(origins = "*")  // Allow React frontend on any port
public class RouteController {

    @Autowired
    private GraphService graphService;

    @Autowired
    private RouteService routeService;

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private MetricsService metricsService;

    // -------------------------------------------------------
    // ROUTE OPTIMIZATION ENDPOINT
    // POST /route/optimize
    // Body: { sourceNodeId, destinationNodeId, weightCost, weightTime, weightCarbon }
    //
    // Runs Dijkstra's algorithm with multi-objective scoring.
    // Returns full RouteResult with path, metrics, and improvement %.
    // -------------------------------------------------------
    @PostMapping("/route/optimize")
    public ResponseEntity<Map<String, Object>> optimizeRoute(@RequestBody Map<String, Object> request) {
        String sourceId = (String) request.get("sourceNodeId");
        String destId   = (String) request.get("destinationNodeId");

        // Validate required inputs
        if (sourceId == null || sourceId.isEmpty()) {
            return ResponseEntity.badRequest().body(errorResponse("sourceNodeId is required"));
        }
        if (destId == null || destId.isEmpty()) {
            return ResponseEntity.badRequest().body(errorResponse("destinationNodeId is required"));
        }

        // Extract optional weight parameters (default to 0 = use service defaults)
        double wc  = getDoubleParam(request, "weightCost",   0.4);
        double wt  = getDoubleParam(request, "weightTime",   0.3);
        double wco = getDoubleParam(request, "weightCarbon", 0.3);

        // Validate weights sum to approximately 1.0
        double weightSum = wc + wt + wco;
        if (weightSum > 0 && Math.abs(weightSum - 1.0) > 0.01) {
            return ResponseEntity.badRequest().body(
                errorResponse("Weights must sum to 1.0 (got " + weightSum + ")"));
        }

        // Ensure graph is initialized
        if (!graphService.isInitialized()) {
            graphService.initializeDefaultGraph();
        }

        // Run Dijkstra's algorithm
        RouteResult result = routeService.findOptimalRoute(sourceId, destId, wc, wt, wco);

        // Record metrics for dashboard
        metricsService.recordRouteOptimization(result);

        // Build response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", result.isRouteFound());
        response.put("algorithm", "Dijkstra's Algorithm (Multi-Objective Scoring)");
        response.put("complexity", "O((V + E) log V)");
        response.put("result", result);
        response.put("ecoPointsEarned", result.getEcoPoints());
        response.put("totalEcoPoints",  metricsService.getTotalEcoPoints());

        if (!result.isRouteFound()) {
            response.put("error", result.getErrorMessage());
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // SIMULATION EVENT ENDPOINT
    // POST /simulate/event
    // Body: { eventType, affectedEdges, sourceNodeId, destinationNodeId, multiplier }
    //
    // Applies a dynamic event (traffic/flood/weather) to edges,
    // then re-runs Dijkstra to show updated optimal route.
    // -------------------------------------------------------
    @PostMapping("/simulate/event")
    public ResponseEntity<Map<String, Object>> simulateEvent(@RequestBody Map<String, Object> request) {
        String eventType = (String) request.get("eventType");
        String sourceId  = (String) request.get("sourceNodeId");
        String destId    = (String) request.get("destinationNodeId");

        if (eventType == null || sourceId == null || destId == null) {
            return ResponseEntity.badRequest().body(
                errorResponse("eventType, sourceNodeId, destinationNodeId are required"));
        }

        // Extract list of affected edges (format: ["FARM_PUNE-WH_PUNE", ...])
        @SuppressWarnings("unchecked")
        List<String> affectedEdges = (List<String>) request.getOrDefault("affectedEdges", Collections.emptyList());

        // Check for preset scenario
        String preset = (String) request.get("preset");
        if (preset != null) {
            if (!graphService.isInitialized()) graphService.initializeDefaultGraph();
            RouteResult result = simulationService.applyPresetScenario(preset, sourceId, destId);
            metricsService.recordSimulation();
            metricsService.recordRouteOptimization(result);
            return ResponseEntity.ok(buildSimulationResponse(result));
        }

        double multiplier = getDoubleParam(request, "multiplier", 1.5);

        // Ensure graph is initialized
        if (!graphService.isInitialized()) {
            graphService.initializeDefaultGraph();
        }

        // Apply event and re-route
        RouteResult result = simulationService.applyEvent(eventType, affectedEdges,
                                                           sourceId, destId, multiplier);
        metricsService.recordSimulation();
        metricsService.recordRouteOptimization(result);

        return ResponseEntity.ok(buildSimulationResponse(result));
    }

    // -------------------------------------------------------
    // RESET SIMULATION ENDPOINT
    // GET /simulate/reset
    //
    // Removes all active simulation events, restoring original graph.
    // -------------------------------------------------------
    @GetMapping("/simulate/reset")
    public ResponseEntity<Map<String, Object>> resetSimulation() {
        simulationService.resetAllEvents();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "All simulation events reset. Graph restored to original state.");
        response.put("activeEvents", simulationService.getActiveEvents());
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // METRICS DASHBOARD ENDPOINT
    // GET /metrics
    //
    // Returns cumulative optimization impact metrics.
    // -------------------------------------------------------
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = metricsService.getDashboardMetrics();
        metrics.put("success", true);
        return ResponseEntity.ok(metrics);
    }

    // -------------------------------------------------------
    // GRAPH VISUALIZATION ENDPOINT
    // GET /graph
    //
    // Returns the full graph structure (nodes + edges) for
    // frontend visualization (D3.js or similar).
    // -------------------------------------------------------
    @GetMapping("/graph")
    public ResponseEntity<Map<String, Object>> getGraph() {
        if (!graphService.isInitialized()) {
            graphService.initializeDefaultGraph();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("nodes",      graphService.getAllNodes());
        response.put("edges",      graphService.getAllEdges());
        response.put("vehicles",   graphService.getAllVehicles());
        response.put("stats",      graphService.getGraphStats());
        response.put("activeEvents", simulationService.getActiveEvents());
        response.put("success", true);

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // INITIALIZE DEFAULT GRAPH ENDPOINT
    // POST /graph/init
    //
    // Resets and re-initializes the default Maharashtra supply chain graph.
    // -------------------------------------------------------
    @PostMapping("/graph/init")
    public ResponseEntity<Map<String, Object>> initGraph() {
        simulationService.resetAllEvents();
        graphService.initializeDefaultGraph();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Default Maharashtra supply chain graph initialized");
        response.put("stats",   graphService.getGraphStats());
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // VEHICLES LIST ENDPOINT
    // GET /vehicles
    // -------------------------------------------------------
    @GetMapping("/vehicles")
    public ResponseEntity<Map<String, Object>> getVehicles() {
        if (!graphService.isInitialized()) graphService.initializeDefaultGraph();
        Map<String, Object> response = new HashMap<>();
        response.put("vehicles", graphService.getAllVehicles());
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // HEALTH CHECK
    // GET /health
    // -------------------------------------------------------
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "SmartAgri Routing Engine");
        response.put("algorithm", "Dijkstra's Algorithm");
        response.put("graphInitialized", graphService.isInitialized());
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // HELPER METHODS
    // -------------------------------------------------------

    private Map<String, Object> buildSimulationResponse(RouteResult result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", result.isRouteFound());
        response.put("simulationApplied", true);
        response.put("activeEvents", simulationService.getActiveEvents());
        response.put("result", result);
        response.put("ecoPointsEarned", result.getEcoPoints());
        response.put("totalEcoPoints",  metricsService.getTotalEcoPoints());
        if (!result.isRouteFound()) {
            response.put("error", result.getErrorMessage());
        }
        return response;
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }

    private double getDoubleParam(Map<String, Object> map, String key, double defaultValue) {
        Object val = map.get(key);
        if (val == null) return defaultValue;
        try {
            return ((Number) val).doubleValue();
        } catch (Exception e) {
            return defaultValue;
        }
    }
}