package com.smartagri.service;

import com.smartagri.model.Edge;
import com.smartagri.model.Node;
import com.smartagri.model.Vehicle;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ============================================================
 * SERVICE: GraphService
 * ============================================================
 * Manages the agricultural supply chain graph data structure.
 *
 * CORE DATA STRUCTURE: Adjacency List
 * -------------------------------------------------------
 * WHY ADJACENCY LIST over Adjacency Matrix?
 *
 *   Adjacency Matrix:
 *     - Space: O(V²) — too large for sparse real-world road networks
 *     - Good for dense graphs only
 *
 *   Adjacency List (our choice):
 *     - Space: O(V + E) — efficient for sparse supply chain graphs
 *     - Faster iteration over neighbors: O(degree) instead of O(V)
 *     - Agricultural networks are SPARSE (each node has few connections)
 *
 * IMPLEMENTATION: HashMap<String, List<Edge>>
 *   Key   = Node ID (String)
 *   Value = List of outgoing Edge objects from that node
 *
 * WHY HashMap?
 *   - O(1) average lookup by node ID (hashing)
 *   - O(V) space for all entries
 *   - Supports dynamic insertion of nodes during graph construction
 *
 * COMPLEXITY SUMMARY:
 *   addNode:       O(1) average
 *   addEdge:       O(1) average
 *   getNeighbors:  O(1) average (returns List<Edge>)
 *   getNode:       O(1) average
 *   Overall Space: O(V + E)
 * ============================================================
 */
@Service
public class GraphService {

    // -------------------------------------------------------
    // PRIMARY DATA STRUCTURE: Adjacency List
    //
    // adjacencyList.get(nodeId) → List<Edge> of outgoing edges
    // This HashMap is the backbone of the entire routing engine.
    // -------------------------------------------------------
    private final Map<String, List<Edge>> adjacencyList = new HashMap<>();

    // Node registry: quick O(1) lookup of Node objects by ID
    private final Map<String, Node> nodeRegistry = new HashMap<>();

    // Vehicle registry: available vehicles for delivery
    private final Map<String, Vehicle> vehicleRegistry = new HashMap<>();

    // -------------------------------------------------------
    // Graph metadata
    // -------------------------------------------------------
    private boolean initialized = false;

    /**
     * Initializes the graph with pre-defined sample agricultural supply chain data.
     * Called at startup — populates nodes, edges, and vehicles.
     *
     * Sample Network: Maharashtra Agricultural Supply Chain
     * Nodes: 8 locations across Maharashtra
     * Edges: 14 directed routes
     */
    public void initializeDefaultGraph() {
        clearGraph();

        // ===== ADD NODES (Vertices) =====
        // Each node represents a real-world supply chain location in Maharashtra

        addNode(new Node("FARM_PUNE",     "Pune Farm Hub",        "FARM",                 18.5204, 73.8567));
        addNode(new Node("FARM_NASHIK",   "Nashik Grape Farm",    "FARM",                 20.0059, 73.7898));
        addNode(new Node("COLD_PUNE",     "Pune Cold Storage",    "COLD_STORAGE",         18.5804, 73.9167));
        addNode(new Node("WH_PUNE",       "Pune Warehouse",       "WAREHOUSE",            18.4529, 73.8503));
        addNode(new Node("WH_MUMBAI",     "Mumbai Warehouse",     "WAREHOUSE",            19.0760, 72.8777));
        addNode(new Node("DC_THANE",      "Thane Dist. Center",   "DISTRIBUTION_CENTER",  19.2183, 72.9781));
        addNode(new Node("MARKET_DADAR",  "Dadar Market",         "RETAIL",               19.0176, 72.8562));
        addNode(new Node("MARKET_VASHI",  "Vashi Market",         "RETAIL",               19.0771, 72.9988));

        // ===== ADD EDGES (Directed Routes) =====
        // Format: (from, to, costINR, timeHours, carbonKg, distanceKm, roadType)
        //
        // Carbon calculated as: distance * emission_factor_for_medium_truck (0.18 kg/km)
        // Cost based on: distance * base_rate + toll + fuel

        // Farm → Cold Storage / Warehouse routes
        addEdge(new Edge("FARM_PUNE",    "COLD_PUNE",    850,   1.2,  10.8, 60,  "STATE_ROAD"));
        addEdge(new Edge("FARM_PUNE",    "WH_PUNE",      650,   0.9,   7.2, 40,  "STATE_ROAD"));
        addEdge(new Edge("FARM_NASHIK",  "WH_PUNE",     1800,   3.5,  28.8, 160, "HIGHWAY"));
        addEdge(new Edge("FARM_NASHIK",  "WH_MUMBAI",   2100,   4.0,  33.3, 185, "HIGHWAY"));

        // Cold Storage → Warehouse routes
        addEdge(new Edge("COLD_PUNE",    "WH_PUNE",      400,   0.5,   3.6, 20,  "STATE_ROAD"));
        addEdge(new Edge("COLD_PUNE",    "WH_MUMBAI",   1600,   2.8,  25.2, 140, "HIGHWAY"));

        // Warehouse → Distribution Center routes
        addEdge(new Edge("WH_PUNE",      "WH_MUMBAI",   1400,   2.5,  19.8, 110, "HIGHWAY"));
        addEdge(new Edge("WH_PUNE",      "DC_THANE",    1500,   2.7,  21.6, 120, "HIGHWAY"));
        addEdge(new Edge("WH_MUMBAI",    "DC_THANE",     500,   0.8,   5.4, 30,  "STATE_ROAD"));
        addEdge(new Edge("WH_MUMBAI",    "MARKET_DADAR", 600,   1.0,   6.3, 35,  "STATE_ROAD"));

        // Distribution Center → Market routes
        addEdge(new Edge("DC_THANE",     "MARKET_DADAR", 450,   0.7,   4.5, 25,  "STATE_ROAD"));
        addEdge(new Edge("DC_THANE",     "MARKET_VASHI", 380,   0.6,   3.6, 20,  "STATE_ROAD"));
        addEdge(new Edge("WH_MUMBAI",    "MARKET_VASHI", 700,   1.2,   8.1, 45,  "STATE_ROAD"));

        // Alternative village road (slower but cheaper/less carbon)
        addEdge(new Edge("FARM_PUNE",    "WH_MUMBAI",   1100,   3.5,  14.4, 80,  "VILLAGE_ROAD"));

        // ===== ADD VEHICLES =====
        addVehicle(new Vehicle("VH001", "Heavy Diesel Truck",   "HEAVY_TRUCK",      5000, 0.25, 18.0, false));
        addVehicle(new Vehicle("VH002", "Medium Truck",         "MEDIUM_TRUCK",     2000, 0.18, 13.0, false));
        addVehicle(new Vehicle("VH003", "Refrigerated Van",     "REFRIGERATED_VAN", 1500, 0.22, 16.0, true));
        addVehicle(new Vehicle("VH004", "Electric Cargo Van",   "ELECTRIC",         1000, 0.05,  8.0, false));
        addVehicle(new Vehicle("VH005", "Motorcycle (Express)", "MOTORCYCLE",        100, 0.08,  5.0, false));

        initialized = true;
    }

    // -------------------------------------------------------
    // GRAPH OPERATIONS
    // -------------------------------------------------------

    /**
     * Adds a node (vertex) to the graph.
     * Creates an empty adjacency list entry for the node.
     * TIME: O(1) average (HashMap put)
     * SPACE: O(1) per node
     */
    public void addNode(Node node) {
        nodeRegistry.put(node.getId(), node);
        // Initialize empty list for this node's outgoing edges
        adjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
    }

    /**
     * Adds a directed edge to the adjacency list.
     * Also ensures the destination node has an entry (even with no outgoing edges).
     * TIME: O(1) average
     */
    public void addEdge(Edge edge) {
        // Ensure source node entry exists in adjacency list
        adjacencyList.computeIfAbsent(edge.getFromNodeId(), k -> new ArrayList<>());
        // Add edge to source node's neighbor list
        adjacencyList.get(edge.getFromNodeId()).add(edge);

        // Ensure destination node has an entry (may have no outgoing edges)
        adjacencyList.computeIfAbsent(edge.getToNodeId(), k -> new ArrayList<>());
    }

    /**
     * Retrieves all outgoing edges from a given node.
     * This is the key operation in Dijkstra's relaxation step.
     * TIME: O(1) average (HashMap get)
     * Returns empty list if node not found (handles disconnected graph gracefully)
     */
    public List<Edge> getNeighbors(String nodeId) {
        return adjacencyList.getOrDefault(nodeId, Collections.emptyList());
    }

    /**
     * Retrieves a node by its ID.
     * TIME: O(1) average
     */
    public Node getNode(String nodeId) {
        return nodeRegistry.get(nodeId);
    }

    /** Returns all nodes in the graph */
    public Collection<Node> getAllNodes() {
        return nodeRegistry.values();
    }

    /** Returns all edges in the graph (flattened from adjacency list) */
    public List<Edge> getAllEdges() {
        List<Edge> allEdges = new ArrayList<>();
        for (List<Edge> edges : adjacencyList.values()) {
            allEdges.addAll(edges);
        }
        return allEdges;
    }

    /** Returns the full adjacency list (for API exposure) */
    public Map<String, List<Edge>> getAdjacencyList() {
        return Collections.unmodifiableMap(adjacencyList);
    }

    /** Retrieves a vehicle by ID */
    public Vehicle getVehicle(String vehicleId) {
        return vehicleRegistry.get(vehicleId);
    }

    /** Returns all registered vehicles */
    public Collection<Vehicle> getAllVehicles() {
        return vehicleRegistry.values();
    }

    /** Adds a vehicle to the registry */
    public void addVehicle(Vehicle vehicle) {
        vehicleRegistry.put(vehicle.getId(), vehicle);
    }

    /**
     * Finds a specific edge between two nodes.
     * Used during simulation to modify edge weights.
     * TIME: O(degree(fromNode))
     */
    public Edge findEdge(String fromNodeId, String toNodeId) {
        List<Edge> edges = adjacencyList.getOrDefault(fromNodeId, Collections.emptyList());
        for (Edge edge : edges) {
            if (edge.getToNodeId().equals(toNodeId)) {
                return edge;
            }
        }
        return null;
    }

    /** Clears all graph data */
    public void clearGraph() {
        adjacencyList.clear();
        nodeRegistry.clear();
        vehicleRegistry.clear();
        initialized = false;
    }

    /** Returns graph statistics */
    public Map<String, Object> getGraphStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("nodeCount", nodeRegistry.size());
        stats.put("edgeCount", getAllEdges().size());
        stats.put("vehicleCount", vehicleRegistry.size());
        stats.put("initialized", initialized);
        stats.put("averageDegree",
            nodeRegistry.isEmpty() ? 0 : (double) getAllEdges().size() / nodeRegistry.size());
        return stats;
    }

    public boolean isInitialized() { return initialized; }
}