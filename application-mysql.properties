package com.petrolbunk.entity;

import jakarta.persistence.*;

/**
 * Represents a single physical pump at the bunk.
 * E.g. Petrol-1..5, Diesel-1..5, Power-1.
 */
@Entity
@Table(name = "pump")
public class Pump {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable label shown in the UI, e.g. "Petrol-1". */
    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FuelType fuelType;

    /** Ordering hint for display (1,2,3...). */
    @Column(nullable = false)
    private int displayOrder;

    public Pump() {
    }

    public Pump(String name, FuelType fuelType, int displayOrder) {
        this.name = name;
        this.fuelType = fuelType;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
