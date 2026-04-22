package com.smartagri.model;

import java.util.Objects;

/**
 * ============================================================
 * MODEL: Node
 * ============================================================
 * Represents a location (vertex) in the agricultural supply chain graph.
 * Examples: Farm, Warehouse, Distribution Center, Retail Market, Cold Storage.
 *
 * WHY THIS DESIGN:
 * - Used as keys in HashMap<Node, List<Edge>> (adjacency list)
 * - Requires equals() + hashCode() for correct HashMap behavior
 * - Comparable for PriorityQueue ordering in Dijkstra
 *
 * SPACE COMPLEXITY: O(V) total for all nodes, where V = number of vertices
 * ============================================================
 */
public class Node implements Comparable<Node> {

    // Unique identifier for graph operations and HashMap keying
    private String id;

    // Human-readable name (e.g., "Pune Farm", "Mumbai Warehouse")
    private String name;

    // Node type categorizes behavior in supply chain
    // Values: FARM, WAREHOUSE, DISTRIBUTION_CENTER, RETAIL, COLD_STORAGE
    private String type;

    // Geographic coordinates for distance estimation and UI rendering
    private double latitude;
    private double longitude;

    // -------------------------------------------------------
    // For Dijkstra's algorithm: tentative distance from source.
    // This is NOT the final shortest distance — it's updated
    // during relaxation. Default = Double.MAX_VALUE (infinity).
    // -------------------------------------------------------
    private double tentativeScore = Double.MAX_VALUE;

    // Default constructor required for JSON deserialization
    public Node() {}

    public Node(String id, String name, String type, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // -------------------------------------------------------
    // Comparable interface: used by PriorityQueue (min-heap)
    // in Dijkstra's algorithm to always extract the node
    // with the lowest tentative score first.
    // TIME: O(log V) per extraction from heap
    // -------------------------------------------------------
    @Override
    public int compareTo(Node other) {
        return Double.compare(this.tentativeScore, other.tentativeScore);
    }

    // -------------------------------------------------------
    // equals() and hashCode() based on 'id' only.
    // This is CRITICAL for HashMap<Node, List<Edge>> to work correctly.
    // Two nodes with same id must be treated as the same key.
    // -------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;
        Node node = (Node) o;
        return Objects.equals(id, node.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Node{id='" + id + "', name='" + name + "', type='" + type + "'}";
    }

    // ===================== Getters & Setters =====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getTentativeScore() { return tentativeScore; }
    public void setTentativeScore(double tentativeScore) { this.tentativeScore = tentativeScore; }
}