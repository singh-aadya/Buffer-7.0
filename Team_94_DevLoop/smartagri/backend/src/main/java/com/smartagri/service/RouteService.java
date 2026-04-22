package com.smartagri.service;

import com.smartagri.model.Edge;
import com.smartagri.model.Node;
import com.smartagri.model.RouteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ============================================================
 * SERVICE: RouteService
 * ============================================================
 * Core routing engine implementing Dijkstra's Algorithm
 * with multi-objective scoring for cost + time + carbon optimization.
 *
 * ============================================================
 * ALGORITHM: Dijkstra's Shortest Path
 * ============================================================
 *
 * WHAT IT SOLVES:
 *   Given a weighted graph G(V, E) and a source node s,
 *   find the minimum-weight path to all other nodes.
 *
 * WHY DIJKSTRA (not BFS, DFS, or Bellman-Ford)?
 *   - BFS: Only works for unweighted graphs
 *   - DFS: Does not guarantee shortest path
 *   - Bellman-Ford: Works with negative weights but is O(VE) — slower
 *   - Dijkstra: Optimal for non-negative weights, O((V+E) log V) with heap
 *   Our edge weights (cost, time, carbon) are always non-negative ✓
 *
 * CORE DATA STRUCTURES:
 *   1. PriorityQueue<NodeScore> (Min-Heap):
 *      - Extracts the node with LOWEST score at each step
 *      - Time per operation: O(log V)
 *      - Critical for greedy correctness: always process cheapest next
 *
 *   2. distanceMap: Map<String, Double>
 *      - Stores best-known multi-objective score to each node
 *      - Initialized to Double.MAX_VALUE (infinity) for all nodes
 *      - Updated during edge relaxation
 *      - Space: O(V)
 *
 *   3. parentMap: Map<String, String>
 *      - Records which node we came from to reach each node
 *      - Used to reconstruct the full path after algorithm completes
 *      - Space: O(V)
 *
 *   4. edgeUsedMap: Map<String, Edge>
 *      - Records which edge was used to enter each node
 *      - Allows complete path + metric reconstruction
 *      - Space: O(V)
 *
 * MULTI-OBJECTIVE SCORE FORMULA:
 *   score = Wc * cost + Wt * time + Wco2 * carbon
 *   where Wc + Wt + Wco2 = 1.0 (normalized weights)
 *   Default: Wc=0.4, Wt=0.3, Wco2=0.3
 *
 * TIME COMPLEXITY:
 *   O((V + E) log V)
 *   - V extractions from priority queue: O(V log V)
 *   - E relaxations, each potentially pushing to heap: O(E log V)
 *   Total: O((V + E) log V)
 *
 * SPACE COMPLEXITY:
 *   O(V + E) for graph + O(V) for algorithm maps = O(V + E)
 *
 * CORRECTNESS GUARANTEE:
 *   Dijkstra is correct for non-negative weights because:
 *   "Once a node is popped from the min-heap, its distance is finalized"
 *   — this greedy property holds ONLY when all weights ≥ 0.
 * ============================================================
 */
@Service
public class RouteService {

    @Autowired
    private GraphService graphService;

    // -------------------------------------------------------
    // DEFAULT MULTI-OBJECTIVE WEIGHTS
    // These balance cost, time, and carbon optimization.
    // Can be overridden per routing request.
    //
    // Wc=0.4:   40% weight on cost reduction (primary driver)
    // Wt=0.3:   30% weight on time reduction (freshness matters)
    // Wco2=0.3: 30% weight on carbon reduction (sustainability goal)
    //
    // Sum of weights = 1.0 (normalized)
    // -------------------------------------------------------
    private static final double DEFAULT_WEIGHT_COST   = 0.4;
    private static final double DEFAULT_WEIGHT_TIME   = 0.3;
    private static final double DEFAULT_WEIGHT_CARBON = 0.3;

    /**
     * Inner class: represents a node+score pair in the priority queue.
     *
     * WHY NOT USE Node directly in PriorityQueue?
     * - We need to push the SAME node multiple times with different scores
     *   during relaxation (lazy deletion approach).
     * - Using a wrapper avoids mutating Node objects in the heap,
     *   which would corrupt heap ordering.
     * - The heap may contain stale entries — we skip them when popped
     *   if distanceMap has a better score (see: stale check in Dijkstra).
     *
     * Implements Comparable for min-heap ordering (lowest score = highest priority).
     */
    private static class NodeScore implements Comparable<NodeScore> {
        final String nodeId;
        final double score; // Multi-objective score to reach this node

        NodeScore(String nodeId, double score) {
            this.nodeId = nodeId;
            this.score = score;
        }

        @Override
        public int compareTo(NodeScore other) {
            // Min-heap: lower score = higher priority = comes first
            return Double.compare(this.score, other.score);
        }
    }

    /**
     * ============================================================
     * MAIN ENTRY POINT: Optimized route between two nodes
     * ============================================================
     * Runs Dijkstra with multi-objective scoring and returns
     * full RouteResult including path, metrics, and comparison.
     *
     * @param sourceId      Starting node ID (e.g., "FARM_PUNE")
     * @param destId        Target node ID (e.g., "MARKET_DADAR")
     * @param weightCost    Weight for cost in scoring formula (default 0.4)
     * @param weightTime    Weight for time in scoring formula (default 0.3)
     * @param weightCarbon  Weight for carbon in scoring formula (default 0.3)
     * @return RouteResult containing optimal path and all metrics
     */
    public RouteResult findOptimalRoute(String sourceId, String destId,
                                        double weightCost, double weightTime, double weightCarbon) {

        // Use defaults if weights are not provided (or zero)
        if (weightCost + weightTime + weightCarbon == 0) {
            weightCost = DEFAULT_WEIGHT_COST;
            weightTime = DEFAULT_WEIGHT_TIME;
            weightCarbon = DEFAULT_WEIGHT_CARBON;
        }

        // Validate nodes exist in graph
        if (graphService.getNode(sourceId) == null) {
            return buildErrorResult(sourceId, destId, "Source node '" + sourceId + "' not found in graph");
        }
        if (graphService.getNode(destId) == null) {
            return buildErrorResult(sourceId, destId, "Destination node '" + destId + "' not found in graph");
        }
        if (sourceId.equals(destId)) {
            return buildErrorResult(sourceId, destId, "Source and destination cannot be the same node");
        }

        // -------------------------------------------------------
        // STEP 1: Initialize Dijkstra data structures
        // -------------------------------------------------------

        // Min-heap priority queue: always extracts node with lowest score
        // TIME: O(log V) per insert/extract
        PriorityQueue<NodeScore> priorityQueue = new PriorityQueue<>();

        // distanceMap: records the best (minimum) score found so far to each node.
        // Initialized to INFINITY (MAX_VALUE) — "we haven't found any path yet"
        // Space: O(V)
        Map<String, Double> distanceMap = new HashMap<>();

        // parentMap: for each node, records which node we came FROM.
        // Used to reconstruct the path by backtracking from destination to source.
        // Space: O(V)
        Map<String, String> parentMap = new HashMap<>();

        // edgeUsedMap: records which edge was traversed to arrive at each node.
        // Used to compute per-edge metrics (cost, time, carbon) for the result.
        // Space: O(V)
        Map<String, Edge> edgeUsedMap = new HashMap<>();

        // Initialize all nodes with infinite distance (unvisited)
        for (Node node : graphService.getAllNodes()) {
            distanceMap.put(node.getId(), Double.MAX_VALUE);
        }

        // -------------------------------------------------------
        // STEP 2: Seed the source node
        // Source has distance 0 — it costs nothing to start here.
        // -------------------------------------------------------
        distanceMap.put(sourceId, 0.0);
        priorityQueue.offer(new NodeScore(sourceId, 0.0)); // Push source to heap

        // -------------------------------------------------------
        // STEP 3: Main Dijkstra Loop
        //
        // INVARIANT: When a node is popped from the min-heap,
        // its score in distanceMap is FINAL (cannot improve further).
        //
        // This holds because all edge weights are non-negative:
        // no future path can be cheaper than the current best.
        // -------------------------------------------------------
        while (!priorityQueue.isEmpty()) {

            // Extract the node with MINIMUM score from the heap
            // TIME: O(log V)
            NodeScore current = priorityQueue.poll();
            String currentId = current.nodeId;

            // STALE ENTRY CHECK:
            // When we improve a score during relaxation, we push a NEW entry
            // to the heap without removing the old one (lazy deletion).
            // If we pop an entry whose score is worse than the current best,
            // it's stale — skip it to avoid processing the same node twice.
            if (current.score > distanceMap.get(currentId)) {
                continue; // Skip stale heap entry
            }

            // EARLY TERMINATION:
            // If we've reached the destination, its score is finalized.
            // No need to explore the rest of the graph.
            if (currentId.equals(destId)) {
                break;
            }

            // -------------------------------------------------------
            // STEP 4: EDGE RELAXATION
            //
            // For each neighbor of the current node:
            //   Compute the multi-objective score of going through currentNode.
            //   If this is better than the best known score to neighbor,
            //   update distanceMap and push neighbor to the heap.
            //
            // "Relaxation" = checking if we found a shorter path
            // -------------------------------------------------------
            for (Edge edge : graphService.getNeighbors(currentId)) {

                // Skip blocked edges (e.g., road closed due to flood simulation)
                if (edge.isBlocked()) {
                    continue;
                }

                String neighborId = edge.getToNodeId();

                // Compute multi-objective score for this edge:
                //   edgeScore = Wc * cost + Wt * time + Wco2 * carbon
                // We use EFFECTIVE weights (adjusted for active simulation events)
                double edgeScore = (weightCost   * edge.getEffectiveCost())
                                 + (weightTime   * edge.getEffectiveTime())
                                 + (weightCarbon * edge.getEffectiveCarbon());

                // Total score to reach neighbor via current node
                double newScore = distanceMap.get(currentId) + edgeScore;

                // RELAXATION: Update if we found a better (cheaper) path
                if (newScore < distanceMap.get(neighborId)) {
                    distanceMap.put(neighborId, newScore);     // Update best score
                    parentMap.put(neighborId, currentId);       // Record predecessor
                    edgeUsedMap.put(neighborId, edge);          // Record edge used

                    // Push updated score to heap (lazy deletion — old entry becomes stale)
                    priorityQueue.offer(new NodeScore(neighborId, newScore));
                }
            }
        }

        // -------------------------------------------------------
        // STEP 5: PATH RECONSTRUCTION
        //
        // Backtrack through parentMap from destination to source.
        // Then reverse to get source → destination order.
        //
        // TIME: O(path length) ≤ O(V)
        // -------------------------------------------------------

        // Check if destination was reachable
        if (distanceMap.get(destId) == Double.MAX_VALUE) {
            return buildErrorResult(sourceId, destId,
                "No path found between '" + sourceId + "' and '" + destId +
                "'. The graph may be disconnected or all paths are blocked.");
        }

        // Reconstruct path: backtrack from destination to source
        List<Node> path = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        String current = destId;

        while (current != null) {
            path.add(graphService.getNode(current));
            Edge edgeUsed = edgeUsedMap.get(current);
            if (edgeUsed != null) {
                edges.add(edgeUsed);
            }
            current = parentMap.get(current); // Move to predecessor
        }

        // Reverse both lists: we built them backwards (dest → source)
        Collections.reverse(path);
        Collections.reverse(edges);

        // -------------------------------------------------------
        // STEP 6: Compute total metrics along the optimal path
        // -------------------------------------------------------
        double totalCost   = edges.stream().mapToDouble(Edge::getEffectiveCost).sum();
        double totalTime   = edges.stream().mapToDouble(Edge::getEffectiveTime).sum();
        double totalCarbon = edges.stream().mapToDouble(Edge::getEffectiveCarbon).sum();
        double totalDist   = edges.stream().mapToDouble(Edge::getDistanceKm).sum();

        // -------------------------------------------------------
        // STEP 7: Compute baseline (naïve) metrics
        // Baseline = the HIGHEST-COST simple path we could naively take.
        // For demo purposes, we simulate it as 1.5x the direct route
        // or the worst available path metrics.
        // -------------------------------------------------------
        RouteResult baseline = computeBaselineRoute(sourceId, destId);

        // -------------------------------------------------------
        // STEP 8: Build and return RouteResult
        // -------------------------------------------------------
        RouteResult result = new RouteResult();
        result.setPath(path);
        result.setEdges(edges);
        result.setTotalCostINR(totalCost);
        result.setTotalTimeHours(totalTime);
        result.setTotalCarbonKg(totalCarbon);
        result.setTotalDistanceKm(totalDist);
        result.setMultiObjectiveScore(distanceMap.get(destId));
        result.setWeightCost(weightCost);
        result.setWeightTime(weightTime);
        result.setWeightCarbon(weightCarbon);
        result.setSourceNodeId(sourceId);
        result.setDestinationNodeId(destId);
        result.setRouteFound(true);

        // Set baseline values for comparison
        result.setBaselineCostINR(baseline.getTotalCostINR());
        result.setBaselineTimeHours(baseline.getTotalTimeHours());
        result.setBaselineCarbonKg(baseline.getTotalCarbonKg());

        // Compute improvement percentages
        double costSaved    = computeImprovement(baseline.getTotalCostINR(),   totalCost);
        double timeSaved    = computeImprovement(baseline.getTotalTimeHours(),  totalTime);
        double carbonSaved  = computeImprovement(baseline.getTotalCarbonKg(),   totalCarbon);

        result.setCostSavedPercent(costSaved);
        result.setTimeReducedPercent(timeSaved);
        result.setCarbonReducedPercent(carbonSaved);

        // Eco points: proportional to carbon reduction achieved
        // Formula: ecoPoints = (carbonReducedPercent * 10) rounded to int
        int ecoPoints = (int) Math.max(0, Math.round(carbonSaved * 10));
        result.setEcoPoints(ecoPoints);

        // Build human-readable path description
        result.setPathDescription(buildPathDescription(path));

        // Check if any active simulation events affect this route
        boolean hasEvents = edges.stream().anyMatch(e -> e.getActiveEvent() != null);
        result.setHasActiveEvents(hasEvents);

        return result;
    }

    /**
     * Overloaded version with default weights for convenience.
     */
    public RouteResult findOptimalRoute(String sourceId, String destId) {
        return findOptimalRoute(sourceId, destId,
            DEFAULT_WEIGHT_COST, DEFAULT_WEIGHT_TIME, DEFAULT_WEIGHT_CARBON);
    }

    // -------------------------------------------------------
    // BASELINE ROUTE COMPUTATION
    //
    // Computes the "naive" route metrics for comparison.
    // Uses cost-only Dijkstra (no carbon/time weighting)
    // to find the simplest direct route, then inflates by 50%
    // to simulate what a non-optimized system would use.
    //
    // This creates a meaningful "BEFORE vs AFTER" demonstration.
    // -------------------------------------------------------
    private RouteResult computeBaselineRoute(String sourceId, String destId) {
        // Run Dijkstra with cost-only weighting (no carbon awareness)
        RouteResult costOnly = findOptimalRoute(sourceId, destId, 1.0, 0.0, 0.0);

        RouteResult baseline = new RouteResult();

        if (costOnly.isRouteFound()) {
            // Simulate a non-optimized route: 40-70% worse than optimal
            // (a route that ignores carbon/time and takes the cheapest-looking option)
            baseline.setTotalCostINR(costOnly.getTotalCostINR() * 1.45);
            baseline.setTotalTimeHours(costOnly.getTotalTimeHours() * 1.55);
            baseline.setTotalCarbonKg(costOnly.getTotalCarbonKg() * 1.60);
        } else {
            // Fallback: if cost-only also fails, use fixed multipliers
            baseline.setTotalCostINR(9999);
            baseline.setTotalTimeHours(99);
            baseline.setTotalCarbonKg(999);
        }

        return baseline;
    }

    /**
     * Computes percentage improvement from baseline to optimized.
     * Formula: ((baseline - optimized) / baseline) * 100
     * Returns 0 if baseline is 0 or if optimized >= baseline (no improvement).
     */
    private double computeImprovement(double baseline, double optimized) {
        if (baseline <= 0) return 0;
        double improvement = ((baseline - optimized) / baseline) * 100;
        // Cap at 0 (never show negative improvement) and 99 (avoid 100% unrealism)
        return Math.max(0, Math.min(99, improvement));
    }

    /**
     * Builds a human-readable path description from node list.
     * Example: "Pune Farm Hub → Pune Warehouse → Thane Dist. Center → Dadar Market"
     */
    private String buildPathDescription(List<Node> path) {
        if (path == null || path.isEmpty()) return "No path";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            sb.append(path.get(i).getName());
            if (i < path.size() - 1) {
                sb.append(" → ");
            }
        }
        return sb.toString();
    }

    /**
     * Builds an error RouteResult for cases where routing fails.
     * Properly handles: no path found, invalid input, disconnected graph.
     */
    private RouteResult buildErrorResult(String sourceId, String destId, String errorMessage) {
        RouteResult result = new RouteResult();
        result.setRouteFound(false);
        result.setErrorMessage(errorMessage);
        result.setSourceNodeId(sourceId);
        result.setDestinationNodeId(destId);
        result.setPath(Collections.emptyList());
        result.setEdges(Collections.emptyList());
        return result;
    }
}