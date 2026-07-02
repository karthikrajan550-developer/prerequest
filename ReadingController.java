package com.petrolbunk.entity;

import jakarta.persistence.*;

/** A vehicle under a customer, tracked by vehicle number and fuel type. */
@Entity
@Table(name = "vehicle")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private String vehicleNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FuelType fuelType;

    /** Optional label, e.g. "Lorry", "Owner's car". */
    private String description;

    public Vehicle() {
    }

    public Vehicle(Customer customer, String vehicleNumber, FuelType fuelType) {
        this.customer = customer;
        this.vehicleNumber = vehicleNumber;
        this.fuelType = fuelType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
    public FuelType getFuelType() { return fuelType; }
    public void setFuelType(FuelType fuelType) { this.fuelType = fuelType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
