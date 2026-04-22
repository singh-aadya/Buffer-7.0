package com.smartagri.model;

import java.time.LocalDateTime;

/**
 * ============================================================
 * MODEL: DeliveryOrder
 * ============================================================
 * Represents an agricultural delivery request from source to destination.
 *
 * WHY implements Comparable<DeliveryOrder>:
 * -------------------------------------------------------
 * DeliveryOrder is designed to work with Java's PriorityQueue<DeliveryOrder>.
 * This creates a MIN-HEAP where orders with HIGHER PRIORITY are processed first.
 *
 * Priority is determined by:
 *   1. Urgency Level (CRITICAL > HIGH > MEDIUM > LOW)
 *   2. Perishability (perishable goods degrade faster)
 *   3. Order age (older pending orders should be processed sooner)
 *
 * PriorityQueue TIME COMPLEXITY:
 *   - Insert (offer):  O(log n)
 *   - Extract min (poll): O(log n)
 *   - Peek min: O(1)
 *
 * SPACE COMPLEXITY: O(n) where n = number of pending orders
 *
 * This mimics real-world logistics dispatch systems where
 * emergency food deliveries (e.g., drought relief) take priority
 * over regular market deliveries.
 * ============================================================
 */
public class DeliveryOrder implements Comparable<DeliveryOrder> {

    private String orderId;
    private String sourceNodeId;
    private String destinationNodeId;
    private String vehicleId;

    // Cargo details
    private String cargoType;      // e.g., "Wheat", "Tomatoes", "Milk"
    private double cargoWeightKg;
    private boolean perishable;    // Perishable cargo needs faster routing

    // -------------------------------------------------------
    // URGENCY LEVELS (used in compareTo for PriorityQueue ordering):
    //   CRITICAL = 1 (highest priority — drought relief, emergency)
    //   HIGH     = 2 (time-sensitive perishables)
    //   MEDIUM   = 3 (regular scheduled delivery)
    //   LOW      = 4 (bulk non-perishable, flexible timing)
    // -------------------------------------------------------
    private int urgencyLevel; // 1=CRITICAL, 2=HIGH, 3=MEDIUM, 4=LOW
    private String urgencyLabel;

    // Order lifecycle timestamps
    private LocalDateTime createdAt;
    private LocalDateTime requiredBy;

    // Order status for tracking
    private String status; // PENDING, IN_PROGRESS, DELIVERED, FAILED

    // Reference to the optimized route (populated after routing)
    private RouteResult assignedRoute;

    // Default constructor
    public DeliveryOrder() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    public DeliveryOrder(String orderId, String sourceNodeId, String destinationNodeId,
                         String vehicleId, String cargoType, double cargoWeightKg,
                         boolean perishable, int urgencyLevel, String urgencyLabel) {
        this.orderId = orderId;
        this.sourceNodeId = sourceNodeId;
        this.destinationNodeId = destinationNodeId;
        this.vehicleId = vehicleId;
        this.cargoType = cargoType;
        this.cargoWeightKg = cargoWeightKg;
        this.perishable = perishable;
        this.urgencyLevel = urgencyLevel;
        this.urgencyLabel = urgencyLabel;
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    // -------------------------------------------------------
    // compareTo: defines the ordering in PriorityQueue (min-heap).
    //
    // Logic:
    //   1. Primary sort: urgencyLevel ascending (lower = more urgent)
    //   2. Secondary sort: perishable orders before non-perishable
    //   3. Tertiary sort: older orders first (FIFO within same priority)
    //
    // This ensures CRITICAL perishable orders are always at the top of heap.
    // -------------------------------------------------------
    @Override
    public int compareTo(DeliveryOrder other) {
        // Step 1: Compare urgency (lower number = higher priority)
        if (this.urgencyLevel != other.urgencyLevel) {
            return Integer.compare(this.urgencyLevel, other.urgencyLevel);
        }

        // Step 2: Among same urgency, prioritize perishable goods
        if (this.perishable != other.perishable) {
            return this.perishable ? -1 : 1; // perishable comes first
        }

        // Step 3: Among same urgency and perishability, older orders first
        if (this.createdAt != null && other.createdAt != null) {
            return this.createdAt.compareTo(other.createdAt);
        }

        return 0;
    }

    @Override
    public String toString() {
        return "DeliveryOrder{id='" + orderId + "', from=" + sourceNodeId +
               ", to=" + destinationNodeId + ", urgency=" + urgencyLabel + "}";
    }

    // ===================== Getters & Setters =====================

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId; }

    public String getDestinationNodeId() { return destinationNodeId; }
    public void setDestinationNodeId(String destinationNodeId) { this.destinationNodeId = destinationNodeId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getCargoType() { return cargoType; }
    public void setCargoType(String cargoType) { this.cargoType = cargoType; }

    public double getCargoWeightKg() { return cargoWeightKg; }
    public void setCargoWeightKg(double cargoWeightKg) { this.cargoWeightKg = cargoWeightKg; }

    public boolean isPerishable() { return perishable; }
    public void setPerishable(boolean perishable) { this.perishable = perishable; }

    public int getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(int urgencyLevel) { this.urgencyLevel = urgencyLevel; }

    public String getUrgencyLabel() { return urgencyLabel; }
    public void setUrgencyLabel(String urgencyLabel) { this.urgencyLabel = urgencyLabel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getRequiredBy() { return requiredBy; }
    public void setRequiredBy(LocalDateTime requiredBy) { this.requiredBy = requiredBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public RouteResult getAssignedRoute() { return assignedRoute; }
    public void setAssignedRoute(RouteResult assignedRoute) { this.assignedRoute = assignedRoute; }
}