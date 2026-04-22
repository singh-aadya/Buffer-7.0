package com.smartagri.model;

/**
 * ============================================================
 * MODEL: Vehicle
 * ============================================================
 * Represents a transport vehicle in the supply chain.
 * Vehicle type directly affects:
 * - Carbon emission rate (kg CO2 per km)
 * - Cost per km
 * - Payload capacity (affects whether a delivery can use this vehicle)
 *
 * WHY SEPARATE MODEL:
 * Different vehicle types (truck, refrigerated van, electric vehicle)
 * have drastically different emission factors. This model allows
 * the routing engine to choose the most eco-friendly vehicle for a route.
 *
 * EMISSION FACTORS (kg CO2/km):
 *   Heavy Truck:       ~0.25 kg/km
 *   Medium Truck:      ~0.18 kg/km
 *   Refrigerated Van:  ~0.22 kg/km (extra for refrigeration)
 *   Electric Vehicle:  ~0.05 kg/km (grid-sourced)
 *   Motorcycle:        ~0.08 kg/km
 * ============================================================
 */
public class Vehicle {

    private String id;
    private String name;

    // Vehicle category: HEAVY_TRUCK, MEDIUM_TRUCK, REFRIGERATED_VAN, ELECTRIC, MOTORCYCLE
    private String type;

    // Maximum cargo capacity in kg
    private double capacityKg;

    // -------------------------------------------------------
    // Emission factor: kg of CO2 emitted per kilometer traveled.
    // Used to calculate edge carbon weight:
    //   carbonKg = distanceKm * emissionFactor
    // Lower = more eco-friendly.
    // -------------------------------------------------------
    private double emissionFactorKgPerKm;

    // Base cost per kilometer in INR
    private double costPerKm;

    // Whether vehicle has refrigeration (important for perishables)
    private boolean refrigerated;

    // Default constructor
    public Vehicle() {}

    public Vehicle(String id, String name, String type, double capacityKg,
                   double emissionFactorKgPerKm, double costPerKm, boolean refrigerated) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.capacityKg = capacityKg;
        this.emissionFactorKgPerKm = emissionFactorKgPerKm;
        this.costPerKm = costPerKm;
        this.refrigerated = refrigerated;
    }

    // ===================== Getters & Setters =====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getCapacityKg() { return capacityKg; }
    public void setCapacityKg(double capacityKg) { this.capacityKg = capacityKg; }

    public double getEmissionFactorKgPerKm() { return emissionFactorKgPerKm; }
    public void setEmissionFactorKgPerKm(double emissionFactorKgPerKm) {
        this.emissionFactorKgPerKm = emissionFactorKgPerKm;
    }

    public double getCostPerKm() { return costPerKm; }
    public void setCostPerKm(double costPerKm) { this.costPerKm = costPerKm; }

    public boolean isRefrigerated() { return refrigerated; }
    public void setRefrigerated(boolean refrigerated) { this.refrigerated = refrigerated; }

    @Override
    public String toString() {
        return "Vehicle{id='" + id + "', name='" + name + "', emission=" + emissionFactorKgPerKm + "kg/km}";
    }
}
